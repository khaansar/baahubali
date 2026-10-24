package com.example.analyticsservice.contract;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TestLeaderboardEntry(
        int rank,
        String userName,
        String avatarUrl,
        BigDecimal score,
        BigDecimal maxScore,
        BigDecimal accuracy,
        Integer timeTakenSeconds,
        LocalDateTime firstAttemptAt
) {
}
