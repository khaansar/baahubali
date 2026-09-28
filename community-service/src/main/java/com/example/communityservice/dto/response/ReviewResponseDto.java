package com.example.communityservice.dto.response;

import java.time.Instant;

public record ReviewResponseDto(
        Long reviewId,
        String targetId,
        Integer rating,
        String comment,
        String authorName,
        String authorAvatar,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {}
