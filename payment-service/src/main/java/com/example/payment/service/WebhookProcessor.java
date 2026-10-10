package com.example.payment.service;

import com.example.payment.audit.AuditService;
import com.example.payment.entity.Order;
import com.example.payment.entity.OrderItem;
import com.example.payment.entity.Payment;
import com.example.payment.entity.WebhookEvent;
import com.example.payment.entity.enums.OrderStatus;
import com.example.payment.entity.enums.PaymentStatus;
import com.example.payment.event.DomainEventPublisher;
import com.example.payment.event.EventTypes;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.gateway.PaymentGateway;
import com.example.payment.repository.OrderItemRepository;
import com.example.payment.repository.OrderRepository;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.RefundRepository;
import com.example.payment.repository.WebhookEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookProcessor {
    private final PaymentGateway gateway;
    private final WebhookEventRepository inbox;
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final RefundRepository refunds;
    private final EntitlementService entitlements;
    private final CouponService couponService;
    private final RefundEntitlementPolicy refundPolicy;
    private final DomainEventPublisher events;
    private final AuditService audit;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;
    private final MeterRegistry metrics;

    public void receive(String raw, String signature, String eventId) {
        metrics.counter("webhook_received_total").increment();
        if (!gateway.verifyWebhookSignature(raw, signature)) {
            log.warn("webhook signature invalid");
            throw new PaymentException(ErrorCode.WEBHOOK_SIGNATURE_INVALID, "invalid signature");
        }

        JsonNode root = parse(raw);
        String type = root.path("event").asText();
        String evId = (eventId != null && !eventId.isBlank()) ? eventId : sha(raw);
        WebhookEvent ev;

        try {
            ev = tx.execute(s -> inbox.saveAndFlush(WebhookEvent.of(gateway.name(), evId, type, raw)));
        } catch (DataIntegrityViolationException dup) {
            ev = inbox.findByProviderAndProviderEventId(gateway.name(), evId).orElseThrow();
            if (ev.isProcessed()) { metrics.counter("webhook_duplicate_total").increment(); return; }
        }

        final UUID inboxId = ev.getId();
        try {
            tx.executeWithoutResult(s -> { dispatch(type, root); inbox.markProcessed(inboxId, Instant.now()); });
        } catch (RuntimeException e) {
            tx.executeWithoutResult(s -> inbox.recordFailure(inboxId, truncate(e.getMessage())));
            metrics.counter("webhook_processing_failure_total").increment();
            log.error("webhook processing failed eventId={} type={}", evId, type, e);
            throw e;
        }
    }

    private void dispatch(String type, JsonNode root) {
        switch (type) {
            case "payment.authorized" -> onAuthorized(root.at("/payload/payment/entity"));
            case "payment.captured" -> onCaptured(root.at("/payload/payment/entity"));
            case "payment.failed" -> onFailed(root.at("/payload/payment/entity"));
            case "refund.processed" -> onRefund(root.at("/payload/refund/entity"), true);
            case "refund.failed" -> onRefund(root.at("/payload/refund/entity"), false);
            default -> log.debug("ignored webhook type={}", type);
        }
    }

    private void onCaptured(JsonNode p) {
        String providerOrderId = p.path("order_id").asText();
        var found = payments.lockByProviderOrderId(gateway.name(), providerOrderId);
        if (found.isEmpty()) { log.warn("webhook for unknown provider order {}; acknowledged", providerOrderId); return; }
        applyCapture(found.get(), p.path("id").asText(), p.path("amount").asLong(), p.path("currency").asText(),
            p.path("method").asText(null), "PROVIDER");
    }

    /** Shared by webhook, /verify and reconciliation. Caller must hold the payment row lock. */
    public void applyCapture(Payment pay, String providerPaymentId, long amount, String currency, String method, String actor) {
        if (pay.getStatus() == PaymentStatus.CAPTURED || pay.getStatus() == PaymentStatus.PARTIALLY_REFUNDED
            || pay.getStatus() == PaymentStatus.REFUNDED) return;
        if (amount != pay.getAmount() || currency == null || !currency.equals(pay.getCurrency()))
            throw new PaymentException(ErrorCode.PROVIDER_ERROR, "Provider amount/currency mismatch for payment " + pay.getId());

        Order order = orders.lockById(pay.getOrderId()).orElseThrow();
        OrderStatus previousStatus = order.getStatus();
        Instant now = Instant.now();
        pay.setProviderPaymentId(providerPaymentId); pay.setMethod(method);
        pay.setProviderStatus("captured"); pay.setStatus(PaymentStatus.CAPTURED); pay.setCapturedAt(now);

        if (previousStatus == OrderStatus.EXPIRED || previousStatus == OrderStatus.CANCELLED || previousStatus == OrderStatus.FAILED) {
            order.transitionTo(OrderStatus.PAYMENT_REVIEW);
            events.publish("payment", "PAYMENT", pay.getId(), order.getUserId(), "PAYMENT_CAPTURED_LATE",
                Map.of("orderId", order.getId().toString(), "amount", pay.getAmount(), "currency", pay.getCurrency(), "previousOrderStatus", previousStatus.name()));
            audit.record(actor, null, "PAYMENT_CAPTURED_LATE", "PAYMENT", pay.getId(), order.getId(), "Manual refund or fulfilment review required");
            metrics.counter("payment_late_capture_total").increment();
            log.error("late capture requires review orderId={} paymentId={} providerPaymentId={} previousOrderStatus={}", order.getId(), pay.getId(), providerPaymentId, previousStatus);
            return;
        }

        if (previousStatus != OrderStatus.CREATED && previousStatus != OrderStatus.PAYMENT_PENDING) {
            events.publish("payment", "PAYMENT", pay.getId(), order.getUserId(), "PAYMENT_CAPTURE_REVIEW_REQUIRED",
                Map.of("orderId", order.getId().toString(), "amount", pay.getAmount(), "currency", pay.getCurrency(), "orderStatus", previousStatus.name()));
            audit.record(actor, null, "PAYMENT_CAPTURE_REVIEW_REQUIRED", "PAYMENT", pay.getId(), order.getId(), "Capture received for an order that is not awaiting payment");
            metrics.counter("payment_capture_review_total").increment();
            log.error("capture received for order not awaiting payment orderId={} paymentId={} orderStatus={}", order.getId(), pay.getId(), previousStatus);
            return;
        }

        order.transitionTo(OrderStatus.PAID);
        order.setPaidAt(now);
        couponService.confirm(order.getId());
        for (OrderItem it : orderItems.findByOrderId(order.getId()))
            entitlements.grantPurchase(order.getUserId(), it.getProductType(), it.getProductReferenceId(), order.getId());
        order.transitionTo(OrderStatus.FULFILLED);

        events.publish("payment", "PAYMENT", pay.getId(), order.getUserId(), EventTypes.PAYMENT_CAPTURED,
            Map.of("orderId", order.getId().toString(), "amount", pay.getAmount(), "currency", pay.getCurrency()));
        events.publish("order", "ORDER", order.getId(), order.getUserId(), EventTypes.ORDER_PAID,
            Map.of("orderNumber", order.getOrderNumber(), "total", order.getTotalAmount()));
        audit.record(actor, null, "PAYMENT_CAPTURED", "PAYMENT", pay.getId(), order.getId(), "providerPaymentId=" + providerPaymentId);
        metrics.counter("payment_success_total").increment();
        log.info("payment captured orderId={} paymentId={} providerPaymentId={}", order.getId(), pay.getId(), providerPaymentId);
    }

    private void onAuthorized(JsonNode p) {
        payments.lockByProviderOrderId(gateway.name(), p.path("order_id").asText()).ifPresent(pay -> {
            if (pay.getStatus() != PaymentStatus.CREATED) return;
            pay.setStatus(PaymentStatus.AUTHORIZED); pay.setProviderPaymentId(p.path("id").asText());
            pay.setAuthorizedAt(Instant.now());
            events.publish("payment", "PAYMENT", pay.getId(), null, EventTypes.PAYMENT_AUTHORIZED, Map.of("orderId", pay.getOrderId().toString()));
        });
    }

    private void onFailed(JsonNode p) {
        payments.lockByProviderOrderId(gateway.name(), p.path("order_id").asText()).ifPresent(pay -> {
            if (pay.getStatus() == PaymentStatus.CAPTURED || pay.getStatus() == PaymentStatus.FAILED
                || pay.getStatus() == PaymentStatus.PARTIALLY_REFUNDED || pay.getStatus() == PaymentStatus.REFUNDED) return;
            pay.setStatus(PaymentStatus.FAILED); pay.setProviderPaymentId(p.path("id").asText());
            pay.setFailureCode(p.path("error_code").asText(null));
            pay.setFailureReason(truncate(p.path("error_description").asText(null)));
            events.publish("payment", "PAYMENT", pay.getId(), null, EventTypes.PAYMENT_FAILED, Map.of("orderId", pay.getOrderId().toString()));
            audit.record("PROVIDER", null, "PAYMENT_FAILED", "PAYMENT", pay.getId(), pay.getOrderId(), pay.getFailureCode());
            metrics.counter("payment_failure_total").increment();
        });
    }

    private void onRefund(JsonNode r, boolean ok) {
        refunds.lockByProviderRefundId(r.path("id").asText())
            .ifPresentOrElse(refund -> refundPolicy.settle(refund, ok, "PROVIDER"),
                () -> log.warn("webhook for unknown refund {}; acknowledged", r.path("id").asText()));
    }

    private JsonNode parse(String raw) {
        try { return mapper.readTree(raw); }
        catch (Exception e) { throw new PaymentException(ErrorCode.VALIDATION_FAILED, "Malformed webhook payload"); }
    }

    private static String sha(String s) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    private static String truncate(String s) { return s == null ? null : s.substring(0, Math.min(480, s.length())); }
}