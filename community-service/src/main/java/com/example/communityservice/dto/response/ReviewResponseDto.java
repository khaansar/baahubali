package com.example.communityservice.dto.response;

public record ReviewResponseDto(
        Long reviewId,
        String targetId,
        Integer rating,
        String comment,
        String authorName,
        String authorAvatar
) {}