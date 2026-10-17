package com.example.payment.service;

import com.example.payment.audit.AuditService;
import com.example.payment.client.TestServiceClient;
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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final TestServiceClient testServiceClient;

    public void receive(String raw, String signature, String eventId) {
        metrics.counter("webhook_received_total").increment();

        if (!gateway.verifyWebhookSignature(raw, signature)) {
            log.warn("webhook signature invalid");
            throw new PaymentException(ErrorCode.WEBHOOK_SIGNATURE_INVALID, "invalid signature");
        }

        JsonNode root = parse(raw);
        String type = root.path("event").asText();
        String evId = eventId != null && !eventId.isBlank() ? eventId : sha(raw);
        WebhookEvent ev;

        try {
            ev = tx.execute(s -> inbox.saveAndFlush(WebhookEvent.of(gateway.name(), evId, type, raw)));
        } catch (DataIntegrityViolationException duplicate) {
            ev = inbox.findByProviderAndProviderEventId(gateway.name(), evId).orElseThrow();
            if (ev.isProcessed()) {
                metrics.counter("webhook_duplicate_total").increment();
                return;
            }
        }

        final UUID inboxId = ev.getId();

        try {
            tx.executeWithoutResult(s -> {
                dispatch(type, root);
                inbox.markProcessed(inboxId, Instant.now());
            });
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

    private void onCaptured(JsonNode paymentPayload) {
        String providerOrderId = paymentPayload.path("order_id").asText();
        var found = payments.lockByProviderOrderId(gateway.name(), providerOrderId);

        if (found.isEmpty()) {
            log.warn("webhook for unknown provider order {}; acknowledged", providerOrderId);
            return;
        }

        applyCapture(found.get(), paymentPayload.path("id").asText(), paymentPayload.path("amount").asLong(), paymentPayload.path("currency").asText(), paymentPayload.path("method").asText(null), "PROVIDER");
    }

    public void applyCapture(Payment pay, String providerPaymentId, long amount, String currency, String method, String actor) {
        if (pay.getStatus() == PaymentStatus.CAPTURED || pay.getStatus() == PaymentStatus.PARTIALLY_REFUNDED || pay.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        if (amount != pay.getAmount() || currency == null || !currency.equals(pay.getCurrency())) {
            throw new PaymentException(ErrorCode.PROVIDER_ERROR, "Provider amount/currency mismatch for payment " + pay.getId());
        }

        Order order = orders.lockById(pay.getOrderId()).orElseThrow();
        OrderStatus previousStatus = order.getStatus();
        Instant now = Instant.now();

        pay.setProviderPaymentId(providerPaymentId);
        pay.setMethod(method);
        pay.setProviderStatus("captured");
        pay.setStatus(PaymentStatus.CAPTURED);
        pay.setCapturedAt(now);

        if (previousStatus == OrderStatus.EXPIRED || previousStatus == OrderStatus.CANCELLED || previousStatus == OrderStatus.FAILED) {
            order.transitionTo(OrderStatus.PAYMENT_REVIEW);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderId", order.getId().toString());
            payload.put("amount", pay.getAmount());
            payload.put("currency", pay.getCurrency());
            payload.put("previousOrderStatus", previousStatus.name());

            events.publish("payment", "PAYMENT", pay.getId(), order.getUserId(), "PAYMENT_CAPTURED_LATE", payload);
            audit.record(actor, null, "PAYMENT_CAPTURED_LATE", "PAYMENT", pay.getId(), order.getId(), "Manual refund or fulfilment review required");
            metrics.counter("payment_late_capture_total").increment();
            log.error("late capture requires review orderId={} paymentId={} providerPaymentId={} previousOrderStatus={}", order.getId(), pay.getId(), providerPaymentId, previousStatus);
            return;
        }

        if (previousStatus != OrderStatus.CREATED && previousStatus != OrderStatus.PAYMENT_PENDING) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderId", order.getId().toString());
            payload.put("amount", pay.getAmount());
            payload.put("currency", pay.getCurrency());
            payload.put("orderStatus", previousStatus.name());

            events.publish("payment", "PAYMENT", pay.getId(), order.getUserId(), "PAYMENT_CAPTURE_REVIEW_REQUIRED", payload);
            audit.record(actor, null, "PAYMENT_CAPTURE_REVIEW_REQUIRED", "PAYMENT", pay.getId(), order.getId(), "Capture received for an order that is not awaiting payment");
            metrics.counter("payment_capture_review_total").increment();
            log.error("capture received for order not awaiting payment orderId={} paymentId={} orderStatus={}", order.getId(), pay.getId(), previousStatus);
            return;
        }

        order.transitionTo(OrderStatus.PAID);
        order.setPaidAt(now);
        couponService.confirm(order.getId());

        List<OrderItem> items = orderItems.findByOrderId(order.getId());

        String seriesSlug = items.stream().filter(item -> "TEST_SERIES".equals(item.getProductType().name())).findFirst().map(item -> testServiceClient.seriesSlugOf(item.getProductReferenceId())).orElse(null);

        for (OrderItem item : items) {
            entitlements.grantPurchase(order.getUserId(), item.getProductType(), item.getProductReferenceId(), order.getId());
        }

        order.transitionTo(OrderStatus.FULFILLED);

        Map<String, Object> notificationPayload = new LinkedHashMap<>();
        notificationPayload.put("orderId", order.getId().toString());
        notificationPayload.put("amount", pay.getAmount());
        notificationPayload.put("currency", pay.getCurrency());

        List<Map<String, Object>> purchasedProducts = new ArrayList<>();
        for (OrderItem item : items) {
            Map<String, Object> product = new LinkedHashMap<>();
            product.put("productType", item.getProductType().name());
            product.put("productReferenceId", item.getProductReferenceId().toString());
            product.put("title", item.getProductNameSnapshot());
            product.put("quantity", item.getQuantity());
            purchasedProducts.add(product);
        }

        notificationPayload.put("products", purchasedProducts);

        items.stream().filter(item -> "TEST_SERIES".equals(item.getProductType().name())).findFirst().ifPresent(item -> {
            notificationPayload.put("seriesTitle", item.getProductNameSnapshot());
            notificationPayload.put("seriesId", item.getProductReferenceId().toString());
            if (seriesSlug != null && !seriesSlug.isBlank()) {
                notificationPayload.put("slug", seriesSlug);
            }
        });

        events.publish("payment", "PAYMENT", pay.getId(), order.getUserId(), EventTypes.PAYMENT_CAPTURED, notificationPayload);
        events.publish("order", "ORDER", order.getId(), order.getUserId(), EventTypes.ORDER_PAID, Map.of("orderNumber", order.getOrderNumber(), "total", order.getTotalAmount()));
        audit.record(actor, null, "PAYMENT_CAPTURED", "PAYMENT", pay.getId(), order.getId(), "providerPaymentId=" + providerPaymentId);
        metrics.counter("payment_success_total").increment();
        log.info("payment captured orderId={} paymentId={} providerPaymentId={}", order.getId(), pay.getId(), providerPaymentId);
    }

    private void onAuthorized(JsonNode p) {
        payments.lockByProviderOrderId(gateway.name(), p.path("order_id").asText()).ifPresent(pay -> {
            if (pay.getStatus() != PaymentStatus.CREATED) {
                return;
            }
            pay.setStatus(PaymentStatus.AUTHORIZED);
            pay.setProviderPaymentId(p.path("id").asText());
            pay.setAuthorizedAt(Instant.now());
            events.publish("payment", "PAYMENT", pay.getId(), null, EventTypes.PAYMENT_AUTHORIZED, Map.of("orderId", pay.getOrderId().toString()));
        });
    }

    private void onFailed(JsonNode p) {
        payments.lockByProviderOrderId(gateway.name(), p.path("order_id").asText()).ifPresent(pay -> {
            if (pay.getStatus() == PaymentStatus.CAPTURED || pay.getStatus() == PaymentStatus.FAILED || pay.getStatus() == PaymentStatus.PARTIALLY_REFUNDED || pay.getStatus() == PaymentStatus.REFUNDED) {
                return;
            }
            pay.setStatus(PaymentStatus.FAILED);
            pay.setProviderPaymentId(p.path("id").asText());
            pay.setFailureCode(p.path("error_code").asText(null));
            pay.setFailureReason(truncate(p.path("error_description").asText(null)));

            events.publish("payment", "PAYMENT", pay.getId(), null, EventTypes.PAYMENT_FAILED, Map.of("orderId", pay.getOrderId().toString()));
            audit.record("PROVIDER", null, "PAYMENT_FAILED", "PAYMENT", pay.getId(), pay.getOrderId(), pay.getFailureCode());
            metrics.counter("payment_failure_total").increment();
        });
    }

    private void onRefund(JsonNode refundPayload, boolean successful) {
        refunds.lockByProviderRefundId(refundPayload.path("id").asText()).ifPresentOrElse(
            refund -> refundPolicy.settle(refund, successful, "PROVIDER"),
            () -> log.warn("webhook for unknown refund {}; acknowledged", refundPayload.path("id").asText())
        );
    }

    private JsonNode parse(String raw) {
        try {
            return mapper.readTree(raw);
        } catch (Exception e) {
            throw new PaymentException(ErrorCode.VALIDATION_FAILED, "Malformed webhook payload");
        }
    }

    private static String sha(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String truncate(String value) {
        return value == null ? null : value.substring(0, Math.min(480, value.length()));
    }
}