package com.example.communityservice.service;

import com.example.communityservice.client.AttemptServiceClient;
import com.example.communityservice.client.IamServiceClient;
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
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AttemptServiceClient attemptServiceClient;
    private final IamServiceClient iamServiceClient;

    @Transactional
    public void createReview(ReviewRequestDto request, String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication is required.");
        }

        String targetId = request.targetId().trim();

        if (request.targetType() == Review.TargetType.TEST) {
            ApiResponse<Map<String, Boolean>> attemptCheck = attemptServiceClient.hasUserAttemptedTest(userId, targetId);

            if (attemptCheck == null || attemptCheck.getData() == null || !Boolean.TRUE.equals(attemptCheck.getData().get("hasAttempted"))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must attempt this test before reviewing it.");
            }
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

    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getHydratedReviews(String targetId) {
        String normalizedTargetId = normalizeTargetId(targetId);
        List<Review> reviews = reviewRepository.findAll((root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("targetId"), normalizedTargetId),
                criteriaBuilder.equal(root.get("status"), Review.ReviewStatus.APPROVED),
                criteriaBuilder.isNull(root.get("deletedAt"))
        ), org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        if (reviews.isEmpty()) {
            return List.of();
        }

        List<String> userIds = reviews.stream().map(Review::getUserId).filter(StringUtils::hasText).distinct().toList();
        Map<String, IamServiceClient.UserProfileDto> userProfiles = Map.of();

        if (!userIds.isEmpty()) {
            ApiResponse<Map<String, IamServiceClient.UserProfileDto>> response = iamServiceClient.getUsersBatch(userIds);

            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "IAM service could not return user profiles.");
            }

            userProfiles = response.getData();
        }

        Map<String, IamServiceClient.UserProfileDto> profiles = userProfiles;

        return reviews.stream().map(review -> {
            IamServiceClient.UserProfileDto profile = profiles.get(review.getUserId());
            String authorName = profile != null && StringUtils.hasText(profile.displayName()) ? profile.displayName() : "Anonymous User";
            String authorAvatar = profile != null ? profile.avatarUrl() : null;

            return new ReviewResponseDto(review.getId(), review.getTargetId(), review.getRating(), review.getComment(), authorName, authorAvatar, review.getCreatedAt(), review.getUpdatedAt(), review.getDeletedAt());
        }).toList();
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(String targetId) {
        Double average = reviewRepository.getAverageRatingForTarget(normalizeTargetId(targetId), Review.ReviewStatus.APPROVED);
        return average != null ? average : 0.0;
    }

    @Transactional(readOnly = true)
    public ReviewStatsDto getAdminStats() {
        long total = reviewRepository.countByDeletedAtIsNull();
        long pending = reviewRepository.countByStatusAndDeletedAtIsNull(Review.ReviewStatus.PENDING);
        long approved = reviewRepository.countByStatusAndDeletedAtIsNull(Review.ReviewStatus.APPROVED);
        long rejected = reviewRepository.countByStatusAndDeletedAtIsNull(Review.ReviewStatus.REJECTED);
        Double average = reviewRepository.getAverageApprovedRating(Review.ReviewStatus.APPROVED);

        return new ReviewStatsDto(total, pending, approved, rejected, average == null ? 0.0 : average);
    }

    @Transactional(readOnly = true)
    public AdminReviewPageDto getAdminReviews(Review.ReviewStatus status, Review.TargetType targetType, Integer rating, String search, Instant from, Instant to, Pageable pageable) {
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
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("userId")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("targetId")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("comment")), pattern)
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Review> page = reviewRepository.findAll(specification, pageable);

        return new AdminReviewPageDto(page.getContent().stream().map(AdminReviewResponseDto::from).toList(), page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
    }

    @Transactional(readOnly = true)
    public AdminReviewResponseDto getAdminReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .filter(existing -> existing.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found."));

        return AdminReviewResponseDto.from(review);
    }

    @Transactional
    public AdminReviewResponseDto moderateReview(Long reviewId, ReviewModerationRequestDto request) {
        if (request.status() != Review.ReviewStatus.APPROVED && request.status() != Review.ReviewStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A review can only be approved or rejected by moderation.");
        }

        Review review = reviewRepository.findById(reviewId)
                .filter(existing -> existing.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found."));

        String reason = request.moderationReason() == null ? null : request.moderationReason().trim();

        if (request.status() == Review.ReviewStatus.APPROVED) {
            reason = null;
        }

        review.setStatus(request.status());
        review.setModerationReason(StringUtils.hasText(reason) ? reason : null);
        review.setModeratedBy(UserContextHolder.getUserId());
        review.setModeratedAt(Instant.now());

        return AdminReviewResponseDto.from(reviewRepository.saveAndFlush(review));
    }

    private String normalizeTargetId(String targetId) {
        if (!StringUtils.hasText(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target ID cannot be blank.");
        }

        return targetId.trim();
    }
}
