package com.example.payment.entity;

import com.example.payment.entity.enums.RedemptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "coupon_redemptions")
public class CouponRedemption extends BaseEntity {
    @Column(nullable = false, columnDefinition = "BINARY(16)") private UUID couponId;
    @Column(nullable = false, columnDefinition = "BINARY(16)") private UUID userId;
    @Column(nullable = false, columnDefinition = "BINARY(16)") private UUID orderId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RedemptionStatus status;
    private long discountAmount;
}