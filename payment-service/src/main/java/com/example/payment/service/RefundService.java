package com.example.payment.service;

import com.example.payment.entity.Payment;
import com.example.payment.entity.Refund;
import com.example.payment.enums.EventTypes;
import com.example.payment.enums.PaymentStatus;
import com.example.payment.enums.RefundStatus;
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

    /**
     * Admin-only.
     * Authorization is enforced in the controller + @PreAuthorize.
     */
    public Refund request(UUID adminId, UUID paymentId, long amount, String reason, String idemKey) {

        // Step 1:
        // Reserve refundable amount under row lock,
        // persist REQUESTED.
        // No provider call yet.
        Refund r = tx.execute(s -> {
            Payment pay = payments.lockById(paymentId).orElseThrow(() -> new PaymentException(ErrorCode.NOT_FOUND, "Payment not found"));

            var dup = refunds.findByPaymentIdAndIdempotencyKey(paymentId, idemKey);

            if (dup.isPresent()) {
                return dup.get();
            }

            if (pay.getStatus() != PaymentStatus.CAPTURED && pay.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
                throw new PaymentException(ErrorCode.PAYMENT_NOT_REFUNDABLE, "Payment is not refundable");
            }

            long inFlight = refunds.sumActiveAmount(paymentId);

            if (amount <= 0 || amount > pay.getAmount() - inFlight) {
                throw new PaymentException(ErrorCode.REFUND_EXCEEDS_REFUNDABLE, "Amount exceeds refundable balance");
            }

            Refund n = new Refund();

            n.setPaymentId(paymentId);
            n.setOrderId(pay.getOrderId());
            n.setAmount(amount);
            n.setCurrency(pay.getCurrency());
            n.setReason(reason);
            n.setRequestedBy(adminId);
            n.setIdempotencyKey(idemKey);

            refunds.save(n);

            events.publish("refund", "REFUND", n.getId(), null, EventTypes.REFUND_REQUESTED, Map.of("orderId", n.getOrderId().toString(), "amount", amount));

            audit.record("ADMIN", adminId.toString(), "REFUND_REQUESTED", "REFUND", n.getId(), n.getOrderId(), reason);

            return n;
        });

        // Replay of an already-progressed refund.
        if (r.getStatus() != RefundStatus.REQUESTED) {
            return r;
        }

        // Step 2:
        // Provider call outside transaction.
        // Provider idempotency header =
        // our refund id.
        Payment pay = payments.findById(paymentId).orElseThrow();

        try {
            ProviderRefund pr = gateway.refundPayment(pay.getProviderPaymentId(), amount, r.getId().toString(), Map.of("refundId", r.getId().toString()));

            return tx.execute(s -> {
                Refund cur = refunds.findById(r.getId()).orElseThrow();

                cur.setProviderRefundId(pr.id());
                cur.transitionTo(RefundStatus.PROCESSING);

                if ("processed".equals(pr.status())) {
                    policy.settle(cur, true, "SYSTEM");
                }

                return cur;
            });

        } catch (PaymentException e) {
            if (e.getCode() == ErrorCode.PROVIDER_UNAVAILABLE) {
                log.warn("refund provider call uncertain refundId={}", r.getId());

                return r;
            }

            tx.executeWithoutResult(s -> policy.settle(refunds.findById(r.getId()).orElseThrow(), false, "SYSTEM"));

            throw e;
        }
    }
}