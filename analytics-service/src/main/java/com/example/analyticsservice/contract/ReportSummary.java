package com.example.analyticsservice.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Rank is intentionally not part of the report; older stored reports may still carry it. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReportSummary(
        Double totalScore,
        Double maxScore,
        Integer totalParticipants,
        Double percentile,
        Double accuracy,
        Long timeTakenSeconds
) {
}
