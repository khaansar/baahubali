package com.example.payment.entity;

import com.example.payment.entity.enums.EntitlementSource;
import com.example.payment.entity.enums.EntitlementStatus;
import com.example.payment.entity.enums.ProductType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "entitlements")
public class Entitlement extends BaseEntity {

    @Column(columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(columnDefinition = "BINARY(16)")
    private UUID productReferenceId;

    @Column(columnDefinition = "BINARY(16)")
    private UUID sourceOrderId;

    @Enumerated(EnumType.STRING)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    private EntitlementSource source;

    @Enumerated(EnumType.STRING)
    private EntitlementStatus status = EntitlementStatus.ACTIVE;

    private Instant grantedAt = Instant.now();

    private Instant expiresAt;

    private Instant revokedAt;

    private String revokeReason;
}