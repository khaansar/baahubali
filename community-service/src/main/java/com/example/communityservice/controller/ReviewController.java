package com.example.communityservice.controller;

import com.example.communityservice.config.UserContextHolder;
import com.example.communityservice.dto.ApiResponse;
import com.example.communityservice.dto.request.ReviewModerationRequestDto;
import com.example.communityservice.dto.request.ReviewRequestDto;
import com.example.communityservice.dto.response.AdminReviewPageDto;
import com.example.communityservice.dto.response.AdminReviewResponseDto;
import com.example.communityservice.dto.response.ReviewResponseDto;
import com.example.communityservice.dto.response.ReviewStatsDto;
import com.example.communityservice.entity.Review;
import com.example.communityservice.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Reviews", description = "Endpoints for submitting, retrieving, and moderating user reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "Submit a new review; it enters the moderation queue")
    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitReview(@Valid @RequestBody ReviewRequestDto request) {
        String userId = UserContextHolder.getUserId();

        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User ID missing from Gateway.");
        }

        reviewService.createReview(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED.value(), "Review submitted successfully and is awaiting moderation.", Map.of()));
    }

    @Operation(summary = "Get approved reviews for a specific target")
    @GetMapping("/public/reviews/{targetId}")
    public ResponseEntity<ApiResponse<List<ReviewResponseDto>>> getReviewsByTarget(@PathVariable String targetId) {
        List<ReviewResponseDto> reviews = reviewService.getHydratedReviews(targetId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Reviews retrieved successfully", reviews));
    }

    @Operation(summary = "Get average approved rating for a target")
    @GetMapping("/internal/reviews/{targetId}/average")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getAverageRating(@PathVariable String targetId) {
        Double average = reviewService.getAverageRating(targetId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Average rating retrieved successfully", Map.of("averageRating", average)));
    }

    @Operation(summary = "List reviews for admin moderation")
    @GetMapping("/admin/reviews")
    public ResponseEntity<ApiResponse<AdminReviewPageDto>> getAdminReviews(
            @RequestParam(required = false) Review.ReviewStatus status,
            @RequestParam(required = false) Review.TargetType targetType,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page cannot be negative.");
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 100.");
        }

        Sort safeSort = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, safeSort);
        AdminReviewPageDto result = reviewService.getAdminReviews(status, targetType, rating, search, from, to, pageable);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Reviews retrieved successfully", result));
    }

    @Operation(summary = "Get moderation dashboard statistics")
    @GetMapping("/admin/reviews/stats")
    public ResponseEntity<ApiResponse<ReviewStatsDto>> getAdminReviewStats() {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Review statistics retrieved successfully", reviewService.getAdminStats()));
    }

    @Operation(summary = "Get complete details for one review")
    @GetMapping("/admin/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<AdminReviewResponseDto>> getAdminReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Review retrieved successfully", reviewService.getAdminReview(reviewId)));
    }

    @Operation(summary = "Approve or reject a review")
    @PatchMapping("/admin/reviews/{reviewId}/status")
    public ResponseEntity<ApiResponse<AdminReviewResponseDto>> moderateReview(@PathVariable Long reviewId, @Valid @RequestBody ReviewModerationRequestDto request) {
        AdminReviewResponseDto result = reviewService.moderateReview(reviewId, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Review moderation status updated successfully", result));
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",", 2);
        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        if (!List.of("createdAt", "updatedAt", "rating", "status").contains(property)) {
            property = "createdAt";
        }

        return Sort.by(direction, property);
    }
}
