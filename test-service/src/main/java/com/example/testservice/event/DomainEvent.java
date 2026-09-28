package com.example.testservice.event;

import java.time.Instant;

public record DomainEvent(
        String eventId,
        String eventType,
        Integer eventVersion,
        Instant occurredAt,
        String aggregateType,
        String aggregateId
) {}
