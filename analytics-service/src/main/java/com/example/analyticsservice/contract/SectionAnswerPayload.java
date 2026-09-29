package com.example.analyticsservice.contract;

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
) {
}
