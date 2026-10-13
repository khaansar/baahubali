package com.example.analyticsservice.contract;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Internal database projection. The real user ID is never returned by the public leaderboard API. */
public record TestLeaderboardRow(
        String userId,
        BigDecimal score,
        BigDecimal maxScore,
        BigDecimal accuracyPercentage,
        Integer timeTakenSeconds,
        LocalDateTime firstAttemptAt
) {
}
