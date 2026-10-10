package com.example.payment.idempotency;

import com.example.payment.entity.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "idempotency_keys")
public class IdempotencyKey extends AssignedIdEntity {
    @Column(nullable = false) private String idemKey;
    @Column(nullable = false, columnDefinition = "BINARY(16)") private UUID userId;
    @Column(nullable = false) private String operation;
    @Column(nullable = false) private String requestHash;
    @Column(nullable = false) private String status;
    private Integer responseStatus;
    @Column(columnDefinition = "MEDIUMTEXT") private String responseBody;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private Instant expiresAt;
}