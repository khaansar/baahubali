package com.example.attemptservice.event;

import java.io.Serializable;
import java.time.Instant;

public record AttemptStartedEvent(
        String eventId,
        String attemptId,
        String userId,
        String testId,
        Instant startedAt
) implements Serializable {
}
