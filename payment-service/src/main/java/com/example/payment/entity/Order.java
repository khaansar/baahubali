package com.example.payment.entity;

import com.example.payment.entity.enums.OrderStatus;
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

@Getter @Setter
@Entity @Table(name = "orders")
public class Order extends BaseEntity {
    @Column(nullable = false) private String orderNumber;
    @Column(nullable = false, columnDefinition = "BINARY(16)") private UUID userId;
    @Column(nullable = false) private String currency;
    private long subtotalAmount;
    private long discountAmount;
    private long taxableAmount;
    private long taxAmount;
    private long totalAmount;
    private String couponCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private OrderStatus status;
    @Column(nullable = false) private Instant expiresAt;
    private Instant paidAt;

    public void transitionTo(OrderStatus next) {
        if (status == next) return;
        if (!status.canTransitionTo(next))
            throw new PaymentException(ErrorCode.INVALID_STATE_TRANSITION,
                "Order " + orderNumber + ": " + status + " -> " + next);
        this.status = next;
    }
}