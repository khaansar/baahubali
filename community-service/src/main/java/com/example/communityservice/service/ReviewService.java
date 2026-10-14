package com.example.communityservice.service;

import com.example.communityservice.client.AttemptServiceClient;
import com.example.communityservice.client.IamServiceClient;
import com.example.communityservice.client.PaymentEntitlementClient;
import com.example.communityservice.client.TestSeriesClient;
import com.example.communityservice.config.UserContextHolder;
import com.example.communityservice.dto.ApiResponse;
import com.example.communityservice.dto.request.ReviewModerationRequestDto;
import com.example.communityservice.dto.request.ReviewRequestDto;
import com.example.communityservice.dto.response.AdminReviewPageDto;
import com.example.communityservice.dto.response.AdminReviewResponseDto;
import com.example.communityservice.dto.response.ReviewResponseDto;
import com.example.communityservice.dto.response.ReviewStatsDto;
import com.example.communityservice.entity.Review;
import com.example.communityservice.repository.ReviewRepository;
import feign.FeignException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AttemptServiceClient attemptServiceClient;
    private final IamServiceClient iamServiceClient;
    private final TestSeriesClient testSeriesClient;
    private final PaymentEntitlementClient paymentEntitlementClient;

    @Transactional
    public void createReview(ReviewRequestDto request, String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication is required.");
        }

        if (request == null || request.targetType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review target type is required.");
        }

        if (!StringUtils.hasText(request.targetId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review target ID cannot be blank.");
        }

        String targetId = request.targetId().trim();

        /*
         * TEST reviews require a previous attempt.
         */
        if (request.targetType() == Review.TargetType.TEST) {
            verifyTestAttempt(userId, targetId);
        }

        /*
         * SERIES reviews require entitlement only when the series is paid.
         * This check happens before creating or saving the Review entity.
         */
        if (request.targetType() == Review.TargetType.SERIES) {
            verifySeriesReviewAccess(userId, targetId);
        }

        Review review = Review.builder()
                .userId(userId)
                .targetId(targetId)
                .targetType(request.targetType())
                .rating(request.rating())
                .comment(request.comment() == null ? null : request.comment().trim())
                .status(Review.ReviewStatus.PENDING)
                .build();

        try {
            reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already submitted a review for this " + request.targetType().name().toLowerCase() + ".");
        }
    }

    private void verifyTestAttempt(String userId, String testId) {
        try {
            ApiResponse<Map<String, Boolean>> attemptCheck = attemptServiceClient.hasUserAttemptedTest(userId, testId);

            if (attemptCheck == null || !attemptCheck.isSuccess() || attemptCheck.getData() == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not verify your test attempt. Please try again.");
            }

            if (!Boolean.TRUE.equals(attemptCheck.getData().get("hasAttempted"))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must attempt this test before reviewing it.");
            }
        } catch (FeignException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not verify your test attempt. Please try again.");
        }
    }

    private void verifySeriesReviewAccess(String userId, String targetId) {
        final UUID seriesId;

        try {
            seriesId = UUID.fromString(targetId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid test-series ID.");
        }

        /*
         * Ask test-service whether the series exists and whether its
         * base price is greater than zero.
         */
        final ApiResponse<TestSeriesClient.SeriesReviewAccessDto> accessResponse;

        try {
            accessResponse = testSeriesClient.getSeriesReviewAccess(seriesId);
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test series not found.");
        } catch (FeignException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not verify test-series details. Please try again.");
        }

        if (accessResponse == null || !accessResponse.isSuccess() || accessResponse.getData() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not verify test-series details. Please try again.");
        }

        TestSeriesClient.SeriesReviewAccessDto series = accessResponse.getData();

        if (series.seriesId() == null || !seriesId.equals(series.seriesId())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Test service returned invalid test-series details.");
        }

        /*
         * Free series do not require a payment entitlement.
         */
        if (!series.paid()) {
            return;
        }

        /*
         * The payment service accepts UUID user IDs.
         * Reject malformed identity values instead of bypassing the check.
         */
        final UUID userUuid;

        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The authenticated user ID is invalid.");
        }

        /*
         * Check the existing TEST_SERIES entitlement in payment-service.
         * If payment-service is unavailable, fail closed: do not save.
         */
        final PaymentEntitlementClient.PaymentApiResponse<PaymentEntitlementClient.SeriesEntitlementCheckResponse> entitlementResponse;

        try {
            entitlementResponse = paymentEntitlementClient.checkSeriesEntitlement(
                    new PaymentEntitlementClient.SeriesEntitlementCheckRequest(userUuid, seriesId)
            );
        } catch (FeignException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not verify your test-series entitlement. Please try again.");
        }

        if (entitlementResponse == null || !entitlementResponse.success() || entitlementResponse.data() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not verify your test-series entitlement. Please try again.");
        }

        if (!entitlementResponse.data().allowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must purchase this test series before submitting a review.");
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "community:reviews", key = "#targetId.trim()", unless = "#result == null")
    public List<ReviewResponseDto> getHydratedReviews(String targetId) {
        String normalizedTargetId = normalizeTargetId(targetId);

        /*
         * Only approved reviews are visible through the public endpoint.
         * Pending reviews remain in the moderation queue.
         */
        List<Review> reviews = reviewRepository.findAll(
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.and(
                                criteriaBuilder.equal(root.get("targetId"), normalizedTargetId),
                                criteriaBuilder.notEqual(root.get("status"), Review.ReviewStatus.REJECTED),
                                criteriaBuilder.isNull(root.get("deletedAt"))
                        ),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );

        if (reviews.isEmpty()) {
            return List.of();
        }

        List<String> userIds = reviews.stream()
                .map(Review::getUserId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        Map<String, IamServiceClient.UserProfileDto> userProfiles = Map.of();

        if (!userIds.isEmpty()) {
            ApiResponse<Map<String, IamServiceClient.UserProfileDto>> response = iamServiceClient.getUsersBatch(userIds);

            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "IAM service could not return user profiles.");
            }

            userProfiles = response.getData();
        }

        Map<String, IamServiceClient.UserProfileDto> profiles = userProfiles;

        return reviews.stream()
                .map(review -> {
                    IamServiceClient.UserProfileDto profile = profiles.get(review.getUserId());

                    String authorName = profile != null && StringUtils.hasText(profile.displayName())
                            ? profile.displayName()
                            : "Anonymous User";

                    String authorAvatar = profile != null ? profile.avatarUrl() : null;

                    return new ReviewResponseDto(
                            review.getId(),
                            review.getTargetId(),
                            review.getRating(),
                            review.getComment(),
                            authorName,
                            authorAvatar,
                            review.getCreatedAt(),
                            review.getUpdatedAt(),
                            review.getDeletedAt()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "community:review-average", key = "#targetId.trim()", unless = "#result == null")
    public Double getAverageRating(String targetId) {
        Double average = reviewRepository.getAverageRatingForTarget(
                normalizeTargetId(targetId),
                Review.ReviewStatus.APPROVED
        );

        return average != null ? average : 0.0;
    }

    @Transactional(readOnly = true)
    public ReviewStatsDto getAdminStats() {
        long total = reviewRepository.countByDeletedAtIsNull();

        long pending = reviewRepository.countByStatusAndDeletedAtIsNull(Review.ReviewStatus.PENDING);

        long approved = reviewRepository.countByStatusAndDeletedAtIsNull(Review.ReviewStatus.APPROVED);

        long rejected = reviewRepository.countByStatusAndDeletedAtIsNull(Review.ReviewStatus.REJECTED);

        Double average = reviewRepository.getAverageApprovedRating(Review.ReviewStatus.APPROVED);

        return new ReviewStatsDto(
                total,
                pending,
                approved,
                rejected,
                average == null ? 0.0 : average
        );
    }

    @Transactional(readOnly = true)
    public AdminReviewPageDto getAdminReviews(
            Review.ReviewStatus status,
            Review.TargetType targetType,
            Integer rating,
            String search,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5.");
        }

        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The start date must be before the end date.");
        }

        Specification<Review> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (targetType != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetType"), targetType));
            }

            if (rating != null) {
                predicates.add(criteriaBuilder.equal(root.get("rating"), rating));
            }

            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }

            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";

                predicates.add(
                        criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("userId")), pattern),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("targetId")), pattern),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("comment")), pattern)
                        )
                );
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Review> page = reviewRepository.findAll(specification, pageable);

        List<Review> reviews = page.getContent();

        Map<String, String> userNames = resolveUserNames(reviews);
        Map<String, String> seriesNames = resolveSeriesNames(reviews);

        return new AdminReviewPageDto(
                reviews.stream()
                        .map(review ->
                                AdminReviewResponseDto.from(
                                        review,
                                        userNames.get(review.getUserId()),
                                        review.getTargetType() == Review.TargetType.SERIES
                                                ? seriesNames.get(review.getTargetId())
                                                : null
                                )
                        )
                        .toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    @Transactional(readOnly = true)
    public AdminReviewResponseDto getAdminReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .filter(existing -> existing.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found."));

        return toAdminReviewResponse(review);
    }

    @Transactional
    @CacheEvict(
            cacheNames = {
                    "community:reviews",
                    "community:review-average"
            },
            allEntries = true
    )
    public AdminReviewResponseDto moderateReview(
            Long reviewId,
            ReviewModerationRequestDto request
    ) {
        if (request.status() != Review.ReviewStatus.APPROVED
                && request.status() != Review.ReviewStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A review can only be approved or rejected by moderation.");
        }

        Review review = reviewRepository.findById(reviewId)
                .filter(existing -> existing.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found."));

        String reason = request.moderationReason() == null
                ? null
                : request.moderationReason().trim();

        if (request.status() == Review.ReviewStatus.APPROVED) {
            reason = null;
        }

        review.setStatus(request.status());
        review.setModerationReason(StringUtils.hasText(reason) ? reason : null);
        review.setModeratedBy(UserContextHolder.getUserId());
        review.setModeratedAt(Instant.now());

        Review saved = reviewRepository.saveAndFlush(review);

        return toAdminReviewResponse(saved);
    }

    private String normalizeTargetId(String targetId) {
        if (!StringUtils.hasText(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target ID cannot be blank.");
        }

        return targetId.trim();
    }

    private Map<String, String> resolveUserNames(List<Review> reviews) {
        List<String> userIds = reviews.stream()
                .map(Review::getUserId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return Map.of();
        }

        ApiResponse<Map<String, IamServiceClient.UserProfileDto>> response = iamServiceClient.getUsersBatch(userIds);

        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "IAM service could not return user profiles.");
        }

        return response.getData().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            IamServiceClient.UserProfileDto profile = entry.getValue();

                            return profile != null && StringUtils.hasText(profile.displayName())
                                    ? profile.displayName()
                                    : "Anonymous User";
                        },
                        (first, ignored) -> first
                ));
    }

    private Map<String, String> resolveSeriesNames(List<Review> reviews) {
        List<UUID> seriesIds = reviews.stream()
                .filter(review -> review.getTargetType() == Review.TargetType.SERIES)
                .map(Review::getTargetId)
                .filter(StringUtils::hasText)
                .map(UUID::fromString)
                .distinct()
                .toList();

        if (seriesIds.isEmpty()) {
            return Map.of();
        }

        ApiResponse<List<TestSeriesClient.SeriesNameDto>> response = testSeriesClient.getSeriesNames(seriesIds);

        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Test service could not return series names.");
        }

        return response.getData().stream()
                .collect(Collectors.toMap(
                        item -> item.seriesId().toString(),
                        TestSeriesClient.SeriesNameDto::name,
                        (first, ignored) -> first
                ));
    }

    private AdminReviewResponseDto toAdminReviewResponse(Review review) {
        Map<String, String> userNames = resolveUserNames(List.of(review));
        Map<String, String> seriesNames = resolveSeriesNames(List.of(review));

        String seriesName = review.getTargetType() == Review.TargetType.SERIES
                ? seriesNames.get(review.getTargetId())
                : null;

        return AdminReviewResponseDto.from(
                review,
                userNames.get(review.getUserId()),
                seriesName
        );
    }
}