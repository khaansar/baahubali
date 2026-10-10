package com.example.communityservice.dto.response;

import com.example.communityservice.entity.Review;

import java.time.Instant;

public record AdminReviewResponseDto(
        Long reviewId,
        String userId,
        String targetId,
        Review.TargetType targetType,
        Integer rating,
        String comment,
        Review.ReviewStatus status,
        String moderationReason,
        String moderatedBy,
        Instant moderatedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminReviewResponseDto from(Review review) {
        return new AdminReviewResponseDto(
                review.getId(),
                review.getUserId(),
                review.getTargetId(),
                review.getTargetType(),
                review.getRating(),
                review.getComment(),
                review.getStatus(),
                review.getModerationReason(),
                review.getModeratedBy(),
                review.getModeratedAt(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
