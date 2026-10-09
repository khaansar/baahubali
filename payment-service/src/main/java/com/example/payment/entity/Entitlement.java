package com.example.payment.entity;

import com.example.payment.enums.EntitlementSource;
import com.example.payment.enums.EntitlementStatus;
import com.example.payment.enums.ProductType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter @Entity @Table(name = "entitlements")
public class Entitlement extends BaseEntity {
    @Column(columnDefinition = "BINARY(16)") private UUID userId, productReferenceId, sourceOrderId;
    @Enumerated(EnumType.STRING) private ProductType productType;
    @Enumerated(EnumType.STRING) private EntitlementSource source;
    @Enumerated(EnumType.STRING) private EntitlementStatus status = EntitlementStatus.ACTIVE;
    private Instant grantedAt = Instant.now(), expiresAt, revokedAt;
    private String revokeReason;
}