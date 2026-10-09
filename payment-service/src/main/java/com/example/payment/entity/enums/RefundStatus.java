package com.example.payment.entity.enums;

public enum RefundStatus {
    REQUESTED, PROCESSING, SUCCEEDED, FAILED;
    public boolean canTransitionTo(RefundStatus next) {
        return switch (this) {
            case REQUESTED  -> next == PROCESSING || next == FAILED;
            case PROCESSING -> next == SUCCEEDED || next == FAILED;
            case SUCCEEDED, FAILED -> false;
        };
    }
}