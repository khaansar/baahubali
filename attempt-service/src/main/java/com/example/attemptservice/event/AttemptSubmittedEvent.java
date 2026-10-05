package com.example.attemptservice.event;

import java.io.Serializable;
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
        List<TopicAnswerPayload> topicAnswers,
        Instant timestamp
) implements Serializable {

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
        this(eventId, attemptId, userId, categoryId, testSeriesId, testId, timeTakenSeconds, sectionAnswers, List.of(), timestamp);
    }

    public record SectionAnswerPayload(
            String sectionId,
            String sectionName,
            Double score,
            Double maxScore,
            Integer totalQuestions,
            Integer correct,
            Integer incorrect,
            Integer unattempted,
            Double accuracy,
            Long timeSpentSeconds
    ) implements Serializable {
    }

    public record TopicAnswerPayload(
            String topic,
            Integer totalQuestions,
            Integer correct,
            Integer incorrect,
            Integer unattempted,
            Double accuracy,
            Long timeSpentSeconds
    ) implements Serializable {
    }
}