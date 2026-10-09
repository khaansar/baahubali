package com.example.payment.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.payment.entity.enums.OrderStatus;
import com.example.payment.exception.PaymentException;
import org.junit.jupiter.api.Test;

class OrderStatusTransitionTest {

    @Test
    void cannotLeaveRefunded() {
        assertFalse(OrderStatus.REFUNDED.canTransitionTo(OrderStatus.PAID));
    }

    @Test
    void cannotPayCancelled() {
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PAID));
    }

    @Test
    void lateCaptureOnExpiredRequiresReview() {
        assertTrue(OrderStatus.EXPIRED.canTransitionTo(OrderStatus.PAYMENT_REVIEW));
        assertFalse(OrderStatus.EXPIRED.canTransitionTo(OrderStatus.PAID));
    }

    @Test
    void orderThrowsOnInvalid() {
        Order o = new Order();

        o.setOrderNumber("O");
        o.setStatus(OrderStatus.REFUNDED);

        assertThrows(PaymentException.class, () -> o.transitionTo(OrderStatus.PAID));
    }
}