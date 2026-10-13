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
        Instant startedAt,
        Long timeTakenSeconds,
        List<SectionAnswerPayload> sectionAnswers,
        List<TopicAnswerPayload> topicAnswers,
        Instant timestamp
) {
    public AttemptSubmittedEvent(
            String eventId,
            String attemptId,
            String userId,
            String categoryId,
            String testSeriesId,
            String testId,
            Long timeTakenSeconds,
            List<SectionAnswerPayload> sectionAnswers,
            List<TopicAnswerPayload> topicAnswers,
            Instant timestamp
    ) {
        this(eventId, attemptId, userId, categoryId, testSeriesId, testId, null, timeTakenSeconds, sectionAnswers, topicAnswers, timestamp);
    }

    public AttemptSubmittedEvent(
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
        this(eventId, attemptId, userId, categoryId, testSeriesId, testId, null, timeTakenSeconds, sectionAnswers, List.of(), timestamp);
    }
}
