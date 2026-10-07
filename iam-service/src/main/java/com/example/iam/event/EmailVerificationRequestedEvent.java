package com.example.iam.event;

import java.time.Instant;
import java.util.Map;

public record EmailVerificationRequestedEvent(
        String eventId,
        String eventType,
        Integer version,
        Instant occurredAt,
        String source,
        String userId,
        String correlationId,
        String recipient,
        Map<String, Object> payload
) {
}