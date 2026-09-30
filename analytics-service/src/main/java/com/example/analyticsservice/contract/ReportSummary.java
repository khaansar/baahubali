package com.example.analyticsservice.contract;

public record ReportSummary(
        Double totalScore,
        Double maxScore,
        Integer rank,
        Integer totalParticipants,
        Double percentile,
        Double accuracy,
        Long timeTakenSeconds
) {
}
