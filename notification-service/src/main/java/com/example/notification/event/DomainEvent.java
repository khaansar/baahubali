package com.example.notification.event;

import java.time.Instant;
import java.util.Map;

public record DomainEvent(
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
