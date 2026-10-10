package com.example.payment.service;

import com.example.payment.audit.AuditService;
import com.example.payment.entity.Order;
import com.example.payment.entity.Payment;
import com.example.payment.entity.Refund;
import com.example.payment.entity.enums.OrderStatus;
import com.example.payment.entity.enums.PaymentStatus;
import com.example.payment.entity.enums.RefundStatus;
import com.example.payment.event.DomainEventPublisher;
import com.example.payment.event.EventTypes;
import com.example.payment.repository.OrderRepository;
import com.example.payment.repository.PaymentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RefundEntitlementPolicy {

    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final EntitlementService entitlements;
    private final DomainEventPublisher events;
    private final AuditService audit;
    private final MeterRegistry metrics;

    /** Single place where refund result -> payment/order/entitlement consequences are decided. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void settle(Refund refund, boolean succeeded, String actor) {
        if (refund.getStatus() == RefundStatus.SUCCEEDED || refund.getStatus() == RefundStatus.FAILED) return;

        if (refund.getStatus() == RefundStatus.REQUESTED) refund.transitionTo(RefundStatus.PROCESSING);
        refund.setProcessedAt(Instant.now());

        if (!succeeded) {
            refund.transitionTo(RefundStatus.FAILED);
            events.publish("refund", "REFUND", refund.getId(), null, EventTypes.REFUND_FAILED, Map.of("orderId", refund.getOrderId().toString()));
            audit.record(actor, null, "REFUND_FAILED", "REFUND", refund.getId(), refund.getOrderId(), null);
            metrics.counter("refund_failure_total").increment();
            return;
        }

        refund.transitionTo(RefundStatus.SUCCEEDED);
        Payment pay = payments.lockById(refund.getPaymentId()).orElseThrow();
        Order order = orders.lockById(refund.getOrderId()).orElseThrow();
        pay.setRefundedAmount(pay.getRefundedAmount() + refund.getAmount());

        boolean full = pay.getRefundedAmount() >= pay.getAmount();
        pay.setStatus(full ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        order.transitionTo(full ? OrderStatus.REFUNDED : OrderStatus.PARTIALLY_REFUNDED);

        if (full) entitlements.revokeForOrder(order.getId(), "Full refund", actor, null);

        events.publish("refund", "REFUND", refund.getId(), order.getUserId(), EventTypes.REFUND_SUCCEEDED,
            Map.of("orderId", order.getId().toString(), "amount", refund.getAmount(), "full", full));
        audit.record(actor, null, "REFUND_SUCCEEDED", "REFUND", refund.getId(), order.getId(), full ? "full" : "partial");
        metrics.counter("refund_total").increment();
    }
}