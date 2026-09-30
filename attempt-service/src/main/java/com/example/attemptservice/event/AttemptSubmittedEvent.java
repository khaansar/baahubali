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
        Instant timestamp
) implements Serializable {

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
}