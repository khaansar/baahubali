package com.example.payment.entity;

import com.example.payment.entity.enums.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter @Entity @Table(name = "payments")
public class Payment extends BaseEntity {
    @Column(nullable = false, columnDefinition = "BINARY(16)") private UUID orderId;
    @Column(nullable = false) private String provider;          // "RAZORPAY"
    @Column(nullable = false) private String providerOrderId;
    private String providerPaymentId, providerStatus, method, failureCode, failureReason;
    private long amount; private String currency;
    private long refundedAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentStatus status = PaymentStatus.CREATED;
    private Instant authorizedAt, capturedAt;
    public long refundableAmount() { return amount - refundedAmount; }
}