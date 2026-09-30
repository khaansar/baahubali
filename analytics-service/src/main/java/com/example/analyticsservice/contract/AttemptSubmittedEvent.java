package com.example.analyticsservice.contract;

import java.time.Instant;
import java.util.List;

public record AttemptSubmittedEvent(
        String eventId,
        String attemptId,
        String userId,
        String categoryId,
        String testSeriesId,
        String testId,
        Long timeTakenSeconds,
        List<SectionAnswerPayload> sectionAnswers,
        Instant timestamp
) {
}