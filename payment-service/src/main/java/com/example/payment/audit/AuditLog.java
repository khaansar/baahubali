package com.example.payment.audit;

import com.example.payment.entity.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "audit_logs")
public class AuditLog extends AssignedIdEntity {
    @Column(nullable = false) private String actorType;          // USER | ADMIN | SYSTEM | PROVIDER
    private String actorId;
    @Column(nullable = false) private String action;
    @Column(nullable = false) private String aggregateType;
    @Column(nullable = false) private String aggregateId;
    @Column(columnDefinition = "BINARY(16)") private UUID orderId;
    private String reason;
    @Column(columnDefinition = "TEXT") private String details;
    private String sourceIp;
    private String correlationId;
    @Column(nullable = false, updatable = false) private Instant occurredAt = Instant.now();
}