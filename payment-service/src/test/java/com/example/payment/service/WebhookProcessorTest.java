package com.example.payment.service;

import com.example.payment.TestTx;
import com.example.payment.audit.AuditService;
import com.example.payment.entity.Order;
import com.example.payment.entity.OrderItem;
import com.example.payment.entity.Payment;
import com.example.payment.entity.WebhookEvent;
import com.example.payment.entity.enums.OrderStatus;
import com.example.payment.entity.enums.PaymentStatus;
import com.example.payment.entity.enums.ProductType;
import com.example.payment.event.DomainEventPublisher;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.gateway.PaymentGateway;
import com.example.payment.repository.OrderItemRepository;
import com.example.payment.repository.OrderRepository;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.RefundRepository;
import com.example.payment.repository.WebhookEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebhookProcessorTest {
    @Mock PaymentGateway gateway; @Mock WebhookEventRepository inbox; @Mock PaymentRepository payments;
    @Mock OrderRepository orders; @Mock OrderItemRepository orderItems; @Mock RefundRepository refunds;
    @Mock EntitlementService entitlements; @Mock CouponService coupons; @Mock RefundEntitlementPolicy refundPolicy;
    @Mock DomainEventPublisher events; @Mock AuditService audit;
    WebhookProcessor processor;

    static final String CAPTURED = """
        {"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_1","order_id":"order_1","amount":49900,"currency":"INR","method":"upi"}}}}""";

    @BeforeEach
    void setUp() {
        when(gateway.name()).thenReturn("RAZORPAY");
        when(gateway.verifyWebhookSignature(any(), any())).thenReturn(true);
        when(inbox.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        processor = new WebhookProcessor(gateway, inbox, payments, orders, orderItems, refunds, entitlements, coupons,
            refundPolicy, events, audit, new ObjectMapper(), TestTx.template(), new SimpleMeterRegistry());
    }

    Payment payment(PaymentStatus st) {
        Payment p = new Payment(); p.setOrderId(UUID.randomUUID()); p.setProvider("RAZORPAY"); p.setProviderOrderId("order_1");
        p.setAmount(49900); p.setCurrency("INR"); p.setStatus(st); return p;
    }
    Order order(UUID id, OrderStatus st) {
        Order o = new Order(); o.setId(id); o.setOrderNumber("ORD-1"); o.setUserId(UUID.randomUUID()); o.setStatus(st); o.setTotalAmount(49900); return o;
    }

    @Test void invalidSignatureRejectedAndNothingStored() {
        when(gateway.verifyWebhookSignature(any(), any())).thenReturn(false);
        var ex = assertThrows(PaymentException.class, () -> processor.receive(CAPTURED, "bad", "e1"));
        assertEquals(ErrorCode.WEBHOOK_SIGNATURE_INVALID, ex.getCode());
        verifyNoInteractions(inbox, payments);
    }

    @Test void duplicateProcessedEventIsIgnored() {
        when(inbox.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));
        WebhookEvent seen = WebhookEvent.of("RAZORPAY", "e1", "payment.captured", CAPTURED); seen.setProcessed(true);
        when(inbox.findByProviderAndProviderEventId("RAZORPAY", "e1")).thenReturn(Optional.of(seen));
        processor.receive(CAPTURED, "sig", "e1");
        verifyNoInteractions(payments, entitlements);
    }

    @Test void duplicateUnprocessedEventIsRetried() {
        when(inbox.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));
        when(inbox.findByProviderAndProviderEventId("RAZORPAY", "e1"))
            .thenReturn(Optional.of(WebhookEvent.of("RAZORPAY", "e1", "payment.captured", CAPTURED)));
        when(payments.lockByProviderOrderId("RAZORPAY", "order_1")).thenReturn(Optional.empty());
        processor.receive(CAPTURED, "sig", "e1");
        verify(payments).lockByProviderOrderId("RAZORPAY", "order_1");
    }

    @Test void capturedPaymentFulfilsOrderAndGrantsEntitlement() {
        Payment pay = payment(PaymentStatus.CREATED);
        Order o = order(pay.getOrderId(), OrderStatus.PAYMENT_PENDING);
        OrderItem it = new OrderItem(); it.setProductType(ProductType.TEST_SERIES); it.setProductReferenceId(UUID.randomUUID());
        when(payments.lockByProviderOrderId("RAZORPAY", "order_1")).thenReturn(Optional.of(pay));
        when(orders.lockById(pay.getOrderId())).thenReturn(Optional.of(o));
        when(orderItems.findByOrderId(o.getId())).thenReturn(List.of(it));

        processor.receive(CAPTURED, "sig", "e1");

        assertEquals(PaymentStatus.CAPTURED, pay.getStatus());
        assertEquals(OrderStatus.FULFILLED, o.getStatus());
        verify(entitlements).grantPurchase(o.getUserId(), ProductType.TEST_SERIES, it.getProductReferenceId(), o.getId());
        verify(inbox).markProcessed(any(), any());
    }

    @Test void replayOnAlreadyCapturedIsNoop() {
        Payment pay = payment(PaymentStatus.CAPTURED);
        when(payments.lockByProviderOrderId("RAZORPAY", "order_1")).thenReturn(Optional.of(pay));
        processor.receive(CAPTURED, "sig", "e2");
        verify(entitlements, never()).grantPurchase(any(), any(), any(), any());
        verify(orders, never()).lockById(any());
    }

    @Test void amountMismatchRejectedAndNeverGrants() {
        Payment pay = payment(PaymentStatus.CREATED); pay.setAmount(10000);
        when(payments.lockByProviderOrderId("RAZORPAY", "order_1")).thenReturn(Optional.of(pay));
        assertThrows(PaymentException.class, () -> processor.receive(CAPTURED, "sig", "e3"));
        verify(inbox).recordFailure(any(), anyString());
        verify(entitlements, never()).grantPurchase(any(), any(), any(), any());
        assertEquals(PaymentStatus.CREATED, pay.getStatus());
    }

    @Test void lateCaptureOnExpiredOrderStillGrants() {
        Payment pay = payment(PaymentStatus.CREATED);
        Order o = order(pay.getOrderId(), OrderStatus.EXPIRED);
        when(payments.lockByProviderOrderId("RAZORPAY", "order_1")).thenReturn(Optional.of(pay));
        when(orders.lockById(pay.getOrderId())).thenReturn(Optional.of(o));
        when(orderItems.findByOrderId(o.getId())).thenReturn(List.of());
        processor.receive(CAPTURED, "sig", "e4");
        assertEquals(OrderStatus.FULFILLED, o.getStatus());
    }

    @Test void failedAfterCapturedDoesNotRegress() {
        Payment pay = payment(PaymentStatus.CAPTURED);
        when(payments.lockByProviderOrderId("RAZORPAY", "order_1")).thenReturn(Optional.of(pay));
        String failed = """
            {"event":"payment.failed","payload":{"payment":{"entity":{"id":"pay_2","order_id":"order_1"}}}}""";
        processor.receive(failed, "sig", "e5");
        assertEquals(PaymentStatus.CAPTURED, pay.getStatus());
    }
}