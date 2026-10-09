package com.example.payment.service;

import com.example.payment.audit.AuditService;
import com.example.payment.entity.Payment;
import com.example.payment.entity.Refund;
import com.example.payment.entity.enums.PaymentStatus;
import com.example.payment.entity.enums.RefundStatus;
import com.example.payment.event.DomainEventPublisher;
import com.example.payment.event.EventTypes;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.gateway.PaymentGateway;
import com.example.payment.gateway.ProviderRefund;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.RefundRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final PaymentGateway gateway;
    private final RefundEntitlementPolicy policy;
    private final DomainEventPublisher events;
    private final AuditService audit;
    private final TransactionTemplate tx;
    private final MeterRegistry metrics;

    public Refund request(UUID adminId, UUID paymentId, long amount, String reason, String idemKey) {
        if (idemKey == null || idemKey.isBlank()) throw new PaymentException(ErrorCode.VALIDATION_FAILED, "Idempotency-Key is required");

        Refund r = tx.execute(s -> {
            Payment pay = payments.lockById(paymentId).orElseThrow(() -> new PaymentException(ErrorCode.NOT_FOUND, "Payment not found"));
            var dup = refunds.findByPaymentIdAndIdempotencyKey(paymentId, idemKey);
            if (dup.isPresent()) return dup.get();

            if (pay.getStatus() != PaymentStatus.CAPTURED && pay.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
                throw new PaymentException(ErrorCode.PAYMENT_NOT_REFUNDABLE, "Payment is not refundable");
            }

            long inFlight = refunds.sumActiveAmount(paymentId);
            if (amount <= 0 || amount > pay.getAmount() - inFlight) {
                throw new PaymentException(ErrorCode.REFUND_EXCEEDS_REFUNDABLE, "Amount exceeds refundable balance");
            }

            Refund n = new Refund();
            n.setPaymentId(paymentId); n.setOrderId(pay.getOrderId()); n.setAmount(amount);
            n.setCurrency(pay.getCurrency()); n.setReason(reason); n.setRequestedBy(adminId); n.setIdempotencyKey(idemKey);
            refunds.save(n);
            events.publish("refund", "REFUND", n.getId(), null, EventTypes.REFUND_REQUESTED, Map.of("orderId", n.getOrderId().toString(), "amount", amount));
            audit.record("ADMIN", adminId.toString(), "REFUND_REQUESTED", "REFUND", n.getId(), n.getOrderId(), reason);
            return n;
        });

        if (r.getStatus() == RefundStatus.SUCCEEDED || r.getStatus() == RefundStatus.FAILED || r.getProviderRefundId() != null) return r;
        return submitToProvider(r);
    }

    /** Reuses the persisted refund ID as the Razorpay idempotency key after an uncertain outcome. */
    public Refund submitToProvider(Refund r) {
        if (r.getStatus() == RefundStatus.SUCCEEDED || r.getStatus() == RefundStatus.FAILED || r.getProviderRefundId() != null) return r;

        Payment pay = payments.findById(r.getPaymentId()).orElseThrow();
        try {
            ProviderRefund pr = gateway.refundPayment(pay.getProviderPaymentId(), r.getAmount(), r.getId().toString(), Map.of("refundId", r.getId().toString()));
            return tx.execute(s -> {
                Refund cur = refunds.lockById(r.getId()).orElseThrow();
                if (cur.getStatus() == RefundStatus.SUCCEEDED || cur.getStatus() == RefundStatus.FAILED) return cur;
                cur.setProviderRefundId(pr.id());
                if (cur.getStatus() == RefundStatus.REQUESTED) cur.transitionTo(RefundStatus.PROCESSING);
                if ("processed".equals(pr.status())) policy.settle(cur, true, "SYSTEM");
                else if ("failed".equals(pr.status())) policy.settle(cur, false, "SYSTEM");
                return cur;
            });
        } catch (PaymentException e) {
            if (e.getCode() == ErrorCode.PROVIDER_UNAVAILABLE) {
                log.warn("Refund provider outcome uncertain; reconciliation will retry with the same idempotency key refundId={}", r.getId());
                metrics.counter("refund_provider_uncertain_total").increment();
                return refunds.findById(r.getId()).orElse(r);
            }

            tx.executeWithoutResult(s -> {
                Refund cur = refunds.lockById(r.getId()).orElseThrow();
                if (cur.getStatus() == RefundStatus.REQUESTED || cur.getStatus() == RefundStatus.PROCESSING) policy.settle(cur, false, "SYSTEM");
            });
            throw e;
        }
    }
}