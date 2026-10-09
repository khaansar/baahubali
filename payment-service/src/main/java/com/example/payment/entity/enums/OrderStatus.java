package com.example.payment.entity.enums;

public enum OrderStatus {
    CREATED, PAYMENT_PENDING, PAID, FULFILLED, FAILED, EXPIRED, CANCELLED, PARTIALLY_REFUNDED, REFUNDED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case CREATED            -> next == PAYMENT_PENDING || next == CANCELLED || next == EXPIRED || next == FAILED;
            case PAYMENT_PENDING    -> next == PAID || next == FAILED || next == EXPIRED || next == CANCELLED;
            case PAID               -> next == FULFILLED || next == PARTIALLY_REFUNDED || next == REFUNDED;
            case FULFILLED          -> next == PARTIALLY_REFUNDED || next == REFUNDED;
            case PARTIALLY_REFUNDED -> next == PARTIALLY_REFUNDED || next == REFUNDED;
            case EXPIRED, CANCELLED -> next == PAID;
            case FAILED             -> next == PAYMENT_PENDING || next == PAID;
            case REFUNDED           -> false;
        };
    }
}