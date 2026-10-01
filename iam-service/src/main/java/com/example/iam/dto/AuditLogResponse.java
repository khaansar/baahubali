package com.example.iam.dto;

import com.example.iam.entity.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID eventId,
        UUID actorId,
        String actorName,
        String actorRole,
        String action,
        String resourceType,
        String resourceId,
        String service,
        String endpoint,
        String httpMethod,
        Integer statusCode,
        String requestId,
        String metadata,
        Instant createdAt
) {

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getEventId(),
                auditLog.getActorId(),
                auditLog.getActorName(),
                auditLog.getActorRole(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getService(),
                auditLog.getEndpoint(),
                auditLog.getHttpMethod(),
                auditLog.getStatusCode(),
                auditLog.getRequestId(),
                auditLog.getMetadata(),
                auditLog.getCreatedAt()
        );
    }
}