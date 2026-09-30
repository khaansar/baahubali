package com.example.analyticsservice.contract;

public record SectionPerformanceResult(
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
