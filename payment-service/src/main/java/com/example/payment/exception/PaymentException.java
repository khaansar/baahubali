package com.example.payment.exception;

import lombok.Getter;

@Getter public class PaymentException extends RuntimeException {
    private final ErrorCode code;
    public PaymentException(ErrorCode code, String message) { super(message); this.code = code; }
}