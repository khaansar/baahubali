package com.example.payment.entity;

import com.example.payment.entity.enums.RefundStatus;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter @Entity @Table(name = "refunds")
public class Refund extends BaseEntity {
    @Column(columnDefinition = "BINARY(16)") private UUID paymentId, orderId, requestedBy;
    private long amount; private String currency, reason, providerRefundId, idempotencyKey;
    @Enumerated(EnumType.STRING) private RefundStatus status = RefundStatus.REQUESTED;
    private Instant requestedAt = Instant.now(), processedAt;

    public void transitionTo(RefundStatus next) {
        if (status == next) return;
        if (!status.canTransitionTo(next))
            throw new PaymentException(ErrorCode.INVALID_STATE_TRANSITION, "Refund " + getId() + ": " + status + " -> " + next);
        status = next;
    }
}