package com.example.payment.service;

import com.example.payment.audit.AuditService;
import com.example.payment.entity.Order;
import com.example.payment.entity.Payment;
import com.example.payment.entity.Refund;
import com.example.payment.entity.enums.OrderStatus;
import com.example.payment.entity.enums.PaymentStatus;
import com.example.payment.entity.enums.RefundStatus;
import com.example.payment.event.DomainEventPublisher;
import com.example.payment.repository.OrderRepository;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.RefundRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefundEntitlementPolicyTest {
    @Mock PaymentRepository payments; @Mock OrderRepository orders; @Mock RefundRepository refunds;
    @Mock EntitlementService entitlements; @Mock DomainEventPublisher events; @Mock AuditService audit;
    RefundEntitlementPolicy policy;
    Payment pay; Order order;

    @BeforeEach
    void setUp() {
        pay = new Payment(); pay.setAmount(49900); pay.setStatus(PaymentStatus.CAPTURED);
        order = new Order(); order.setOrderNumber("O1"); order.setUserId(UUID.randomUUID()); order.setStatus(OrderStatus.FULFILLED);
        when(payments.lockById(any())).thenReturn(Optional.of(pay));
        when(orders.lockById(any())).thenReturn(Optional.of(order));
        policy = new RefundEntitlementPolicy(payments, orders, refunds, entitlements, events, audit, new SimpleMeterRegistry());
    }
    Refund refund(long amt) {
        Refund r = new Refund(); r.setPaymentId(pay.getId()); r.setOrderId(order.getId()); r.setAmount(amt); r.setStatus(RefundStatus.PROCESSING); return r;
    }

    @Test void partialRefundKeepsEntitlement() {
        policy.settle(refund(20000), true, "PROVIDER");
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, pay.getStatus());
        assertEquals(OrderStatus.PARTIALLY_REFUNDED, order.getStatus());
        verify(entitlements, never()).revokeForOrder(any(), anyString(), anyString(), any());
    }

    @Test void multiplePartialsThatSumToFullRevoke() {
        policy.settle(refund(20000), true, "PROVIDER");
        policy.settle(refund(29900), true, "PROVIDER");
        assertEquals(PaymentStatus.REFUNDED, pay.getStatus());
        assertEquals(OrderStatus.REFUNDED, order.getStatus());
        verify(entitlements).revokeForOrder(any(), anyString(), anyString(), any());
    }

    @Test void settleIsReplaySafe() {
        Refund r = refund(49900);
        policy.settle(r, true, "PROVIDER");
        policy.settle(r, true, "PROVIDER"); 
        assertEquals(49900, pay.getRefundedAmount());
    }

    @Test void failedRefundDoesNotTouchPaymentOrEntitlement() {
        Refund r = refund(100);
        policy.settle(r, false, "PROVIDER");
        assertEquals(RefundStatus.FAILED, r.getStatus());
        assertEquals(0, pay.getRefundedAmount());
        verify(entitlements, never()).revokeForOrder(any(), anyString(), anyString(), any());
    }
}