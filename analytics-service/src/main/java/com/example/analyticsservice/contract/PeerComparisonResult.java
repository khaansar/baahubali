package com.example.analyticsservice.contract;

public record PeerComparisonResult(
        Double topperScore,
        Double averageScore,
        Long topperTimeTakenSeconds,
        Integer rank,
        Integer totalParticipants,
        Double percentile
) {
}
