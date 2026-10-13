package com.example.analyticsservice.contract;

import java.util.List;

public record TestLeaderboardResponse(
        String testSlug,
        int page,
        int pageSize,
        long totalParticipants,
        List<TestLeaderboardEntry> entries
) {
}
