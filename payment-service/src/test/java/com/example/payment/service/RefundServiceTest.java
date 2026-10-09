package com.example.payment.service;

import com.example.payment.TestTx;
import com.example.payment.audit.AuditService;
import com.example.payment.entity.Payment;
import com.example.payment.entity.Refund;
import com.example.payment.entity.enums.PaymentStatus;
import com.example.payment.entity.enums.RefundStatus;
import com.example.payment.event.DomainEventPublisher;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.gateway.PaymentGateway;
import com.example.payment.gateway.ProviderRefund;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefundServiceTest {
    @Mock PaymentRepository payments; @Mock OrderRepository orders; @Mock RefundRepository refunds;
    @Mock PaymentGateway gateway; @Mock RefundEntitlementPolicy policy; @Mock DomainEventPublisher events; @Mock AuditService audit;
    RefundService service;
    UUID admin = UUID.randomUUID();
    Payment pay;

    @BeforeEach
    void setUp() {
        pay = new Payment(); pay.setOrderId(UUID.randomUUID()); pay.setAmount(49900); pay.setCurrency("INR");
        pay.setStatus(PaymentStatus.CAPTURED); pay.setProviderPaymentId("pay_1");
        when(payments.lockById(pay.getId())).thenReturn(Optional.of(pay));
        when(payments.findById(pay.getId())).thenReturn(Optional.of(pay));
        when(refunds.findByPaymentIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(refunds.sumActiveAmount(pay.getId())).thenReturn(0L);
        when(refunds.save(any())).thenAnswer(i -> i.getArgument(0));
        service = new RefundService(payments, orders, refunds, gateway, policy, events, audit, TestTx.template(), new SimpleMeterRegistry());
    }

    @Test void excessiveRefundRejectedBeforeProviderCall() {
        var ex = assertThrows(PaymentException.class, () -> service.request(admin, pay.getId(), 49901, "x", "k1"));
        assertEquals(ErrorCode.REFUND_EXCEEDS_REFUNDABLE, ex.getCode());
        verify(gateway, never()).refundPayment(any(), anyLong(), any(), any());
    }

    @Test void earlierPartialRefundsReduceRefundableBalance() {
        when(refunds.sumActiveAmount(pay.getId())).thenReturn(20000L);
        assertThrows(PaymentException.class, () -> service.request(admin, pay.getId(), 29901, "x", "k2"));
        when(gateway.refundPayment(eq("pay_1"), eq(29900L), anyString(), anyMap())).thenReturn(new ProviderRefund("rfnd_1", "pay_1", 29900, "pending"));
        when(refunds.findById(any())).thenAnswer(i -> Optional.empty());
    }

    @Test void nonCapturedPaymentNotRefundable() {
        pay.setStatus(PaymentStatus.FAILED);
        var ex = assertThrows(PaymentException.class, () -> service.request(admin, pay.getId(), 100, "x", "k3"));
        assertEquals(ErrorCode.PAYMENT_NOT_REFUNDABLE, ex.getCode());
    }

    @Test void duplicateIdempotencyKeyReturnsSameRefundWithoutNewProviderCall() {
        Refund existing = new Refund(); existing.setStatus(RefundStatus.PROCESSING); existing.setAmount(100);
        when(refunds.findByPaymentIdAndIdempotencyKey(pay.getId(), "dup")).thenReturn(Optional.of(existing));
        Refund r = service.request(admin, pay.getId(), 100, "x", "dup");
        assertEquals(existing.getId(), r.getId());
        verify(gateway, never()).refundPayment(any(), anyLong(), any(), any());
    }

    @Test void definitiveProviderRejectionMarksRefundFailed() {
        when(gateway.refundPayment(any(), anyLong(), any(), any()))
            .thenThrow(new PaymentException(ErrorCode.PROVIDER_ERROR, "rejected"));
        when(refunds.findById(any())).thenAnswer(i -> Optional.of(new Refund()));
        assertThrows(PaymentException.class, () -> service.request(admin, pay.getId(), 100, "x", "k4"));
        verify(policy).settle(any(), eq(false), eq("SYSTEM"));
    }

    @Test void providerTimeoutLeavesRefundRequestedForReconciliation() {
        when(gateway.refundPayment(any(), anyLong(), any(), any()))
            .thenThrow(new PaymentException(ErrorCode.PROVIDER_UNAVAILABLE, "timeout"));
        Refund r = service.request(admin, pay.getId(), 100, "x", "k5");
        assertEquals(RefundStatus.REQUESTED, r.getStatus());
        verify(policy, never()).settle(any(), anyBoolean(), any());
    }
    private static boolean anyBoolean() { return org.mockito.ArgumentMatchers.anyBoolean(); }
}