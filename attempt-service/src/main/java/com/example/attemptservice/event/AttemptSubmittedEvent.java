package com.example.attemptservice.event;

import java.io.Serializable;
import java.time.Instant;

public record AttemptSubmittedEvent(
        String eventId,
        String attemptId,
        String userId,
        String testId,
        Instant timestamp
) implements Serializable {
}