package com.example.analyticsservice.contract;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record UserPerformanceResponse(
        PerformanceSummary summary,
        List<AttemptPerformance> attempts,
        List<SectionPerformanceSummary> sectionPerformance
) {

    public record PerformanceSummary(
            long totalAttempts,
            BigDecimal averageScore,
            BigDecimal bestScore,
            BigDecimal averageScorePercentage,
            BigDecimal bestScorePercentage,
            BigDecimal averageAccuracyPercentage,
            BigDecimal averageTimeTakenSeconds,
            BigDecimal averagePercentile
    ) {
    }

    public record AttemptPerformance(
            String attemptId,
            String testId,
            String categoryId,
            String testSeriesId,
            BigDecimal totalScore,
            BigDecimal maxScore,
            BigDecimal scorePercentage,
            BigDecimal accuracyPercentage,
            Integer timeTakenSeconds,
            Integer rank,
            BigDecimal percentile,
            LocalDateTime completedAt
    ) {
    }

    public record SectionPerformanceSummary(
            String sectionId,
            String sectionName,
            long attempts,
            long totalQuestions,
            long correct,
            long incorrect,
            long unattempted,
            BigDecimal averageAccuracyPercentage,
            long totalTimeSpentSeconds,
            BigDecimal averageTimeSpentSeconds
    ) {
    }
}