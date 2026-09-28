package com.example.communityservice.service;

import com.example.communityservice.client.AttemptServiceClient;
import com.example.communityservice.client.IamServiceClient;
import com.example.communityservice.dto.request.ReviewRequestDto;
import com.example.communityservice.dto.response.ReviewResponseDto;
import com.example.communityservice.entity.Review;
import com.example.communityservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AttemptServiceClient attemptServiceClient;
    private final IamServiceClient iamServiceClient;

    /**
     * Creates a review for a test or series.
     *
     * Business rules:
     * - The user must be authenticated.
     * - TEST reviews require the user to have attempted the test.
     * - A user can submit only one review for a given target/type.
     */
    @Transactional
    public void createReview(
            ReviewRequestDto request,
            String userId) {

        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User authentication is required."
            );
        }

        String targetId = request.targetId().trim();

        if (targetId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Target ID cannot be blank."
            );
        }

        /*
         * A user must have attempted a test before reviewing it.
         *
         * Series reviews do not currently require an attempt check.
         */
        if (request.targetType() == Review.TargetType.TEST) {

            boolean hasAttempted =
                    attemptServiceClient.hasUserAttemptedTest(
                            userId,
                            targetId
                    );

            if (!hasAttempted) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You must attempt this test before reviewing it."
                );
            }
        }

        Review review = Review.builder()
                .userId(userId)
                .targetId(targetId)
                .targetType(request.targetType())
                .rating(request.rating())
                .comment(
                        request.comment() == null
                                ? null
                                : request.comment().trim()
                )
                .status(Review.ReviewStatus.APPROVED)
                .build();

        /*
         * The database unique constraint is the final source of truth
         * for duplicate reviews:
         *
         * (user_id, target_id, target_type)
         *
         * Do not rely only on an application-level exists check because
         * two concurrent requests can both pass such a check.
         */
        try {
            reviewRepository.save(review);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already submitted a review for this "
                            + request.targetType()
                            .name()
                            .toLowerCase()
                            + "."
            );
        }
    }

    /**
     * Returns all approved reviews for a target.
     *
     * User information is hydrated from IAM in one batch request
     * instead of making one IAM call per review.
     */
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getHydratedReviews(
            String targetId) {

        String normalizedTargetId = normalizeTargetId(targetId);

        List<Review> reviews =
                reviewRepository.findByTargetIdAndStatus(
                        normalizedTargetId,
                        Review.ReviewStatus.APPROVED
                );

        if (reviews.isEmpty()) {
            return List.of();
        }

        /*
         * Avoid N+1 calls to IAM.
         */
        List<String> userIds = reviews.stream()
                .map(Review::getUserId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        Map<String, IamServiceClient.UserProfileDto> userProfiles =
                userIds.isEmpty()
                        ? Map.of()
                        : iamServiceClient.getUsersBatch(userIds);

        return reviews.stream()
                .map(review -> {

                    IamServiceClient.UserProfileDto profile =
                            userProfiles.get(review.getUserId());

                    String authorName = "Anonymous User";
                    String authorAvatar = null;

                    if (profile != null) {
                        if (profile.displayName() != null
                                && !profile.displayName().isBlank()) {
                            authorName = profile.displayName();
                        }

                        authorAvatar = profile.avatarUrl();
                    }

                    return new ReviewResponseDto(
                            review.getId(),
                            review.getTargetId(),
                            review.getRating(),
                            review.getComment(),
                            authorName,
                            authorAvatar
                    );
                })
                .toList();
    }

    /**
     * Returns the average rating of approved reviews.
     */
    @Transactional(readOnly = true)
    public Double getAverageRating(String targetId) {

        String normalizedTargetId = normalizeTargetId(targetId);

        Double average =
                reviewRepository.getAverageRatingForTarget(
                        normalizedTargetId
                );

        return average != null ? average : 0.0;
    }

    private String normalizeTargetId(String targetId) {

        if (targetId == null || targetId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Target ID cannot be blank."
            );
        }

        return targetId.trim();
    }
}
