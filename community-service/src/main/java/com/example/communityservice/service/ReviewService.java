package com.example.communityservice.service;

import com.example.communityservice.client.AttemptServiceClient;
import com.example.communityservice.client.IamServiceClient;
import com.example.communityservice.dto.request.ReviewRequestDto;
import com.example.communityservice.dto.response.ReviewResponseDto;
import com.example.communityservice.entity.Review;
import com.example.communityservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public void createReview(ReviewRequestDto request, String userId) {
        if (request.targetType() == Review.TargetType.TEST) {
            boolean hasAttempted = attemptServiceClient.hasUserAttemptedTest(userId, request.targetId());
            if (!hasAttempted) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must attempt this test before reviewing it.");
            }
        }

        Review review = Review.builder()
                .userId(userId)
                .targetId(request.targetId())
                .targetType(request.targetType())
                .rating(request.rating())
                .comment(request.comment())
                .status(Review.ReviewStatus.APPROVED) 
                .build();

        reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getHydratedReviews(String targetId) {
        List<Review> reviews = reviewRepository.findByTargetIdAndStatus(targetId, Review.ReviewStatus.APPROVED);
        if (reviews.isEmpty()) return List.of();

        List<String> userIds = reviews.stream()
                .map(Review::getUserId)
                .distinct()
                .toList();

        Map<String, IamServiceClient.UserProfileDto> userProfiles = iamServiceClient.getUsersBatch(userIds);

        return reviews.stream().map(review -> {
            IamServiceClient.UserProfileDto profile = userProfiles.getOrDefault(
                    review.getUserId(), 
                    new IamServiceClient.UserProfileDto(review.getUserId(), "Anonymous User", null)
            );
            
            return new ReviewResponseDto(
                    review.getId(),
                    review.getTargetId(),
                    review.getRating(),
                    review.getComment(),
                    profile.displayName(),
                    profile.avatarUrl()
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(String targetId) {
        return reviewRepository.getAverageRatingForTarget(targetId);
    }
}