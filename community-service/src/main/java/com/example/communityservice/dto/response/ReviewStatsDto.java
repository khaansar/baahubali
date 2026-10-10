package com.example.communityservice.dto.response;

public record ReviewStatsDto(
        long totalReviews,
        long pendingReviews,
        long approvedReviews,
        long rejectedReviews,
        double averageRating
) {}
