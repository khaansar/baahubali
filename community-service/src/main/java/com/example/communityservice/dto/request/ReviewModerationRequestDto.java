package com.example.communityservice.dto.request;

import com.example.communityservice.entity.Review;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewModerationRequestDto(
        @NotNull(message = "A moderation status is required")
        Review.ReviewStatus status,

        @Size(max = 2000, message = "The moderation reason cannot exceed 2000 characters")
        String moderationReason
) {}
