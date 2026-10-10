package com.example.payment.entity;

import com.example.payment.entity.enums.DiscountType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "coupons")
public class Coupon extends BaseEntity {
    @Column(nullable = false) private String code;          // stored UPPERCASE
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DiscountType discountType;
    private long discountValue;                              // PERCENTAGE: basis points; FIXED_AMOUNT: minor units
    private long minimumOrderAmount;
    private Long maximumDiscountAmount;
    @Column(nullable = false) private Instant startsAt;
    @Column(nullable = false) private Instant expiresAt;
    private Integer usageLimit;
    private Integer perUserUsageLimit;
    private int usedCount;
    private boolean stackable;
    private boolean firstOrderOnly;
    @Column(nullable = false) private String status = "ACTIVE";   // ACTIVE | DISABLED

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "coupon_products", joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "product_id", columnDefinition = "BINARY(16)")
    private Set<UUID> productIds = new HashSet<>();           // empty = global
}