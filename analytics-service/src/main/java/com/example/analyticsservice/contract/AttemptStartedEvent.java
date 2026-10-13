package com.example.analyticsservice.contract;

import java.time.Instant;

public record AttemptStartedEvent(
        String eventId,
        String attemptId,
        String userId,
        String testId,
        Instant startedAt
) {
}
