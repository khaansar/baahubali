package com.example.analyticsservice.contract;

import java.util.List;

public record AttemptSubmittedEvent(
        String attemptId,
        String userId,
        String categoryId,
        String testSeriesId,
        String testId,
        Long timeTakenSeconds,
        List<SectionAnswerPayload> sectionAnswers
) {
}
