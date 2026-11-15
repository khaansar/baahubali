package com.example.analyticsservice.contract;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record PeerComparisonResult(
        Double topperScore,
        Double averageScore,
        Long topperTimeTakenSeconds,
        /** Used for persistence only; never exposed in the report payload. */
        @JsonIgnore Integer rank,
        Integer totalParticipants,
        Double percentile
) {
}
