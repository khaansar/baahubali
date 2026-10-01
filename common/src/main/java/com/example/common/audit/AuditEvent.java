package com.example.common.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(
        UUID eventId,
        String actorId,
        String actorRole,
        String action,
        String resourceType,
        String resourceId,
        String service,
        String endpoint,
        String httpMethod,
        int statusCode,
        String requestId,
        Map<String, String> metadata,
        Instant createdAt
) {
}