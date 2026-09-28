package com.example.communityservice.controller;

import com.example.communityservice.config.UserContextHolder;
import com.example.communityservice.dto.ApiResponse;
import com.example.communityservice.dto.request.ReviewRequestDto;
import com.example.communityservice.dto.response.ReviewResponseDto;
import com.example.communityservice.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Endpoints for fetching and submitting user reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Submit a new review (Requires previous attempt)")
    @PostMapping("/public/reviews")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitReview(@Valid @RequestBody ReviewRequestDto request) {
        String userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User ID missing from Gateway.");
        }
        
        reviewService.createReview(request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(HttpStatus.CREATED.value(), "Review submitted successfully", Map.of())
        );
    }

    @Operation(summary = "Get all approved reviews for a specific test or series")
    @GetMapping("/public/reviews/{targetId}")
    public ResponseEntity<ApiResponse<List<ReviewResponseDto>>> getReviewsByTarget(@PathVariable String targetId) {
        List<ReviewResponseDto> reviews = reviewService.getHydratedReviews(targetId);
        
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Reviews retrieved successfully", reviews)
        );
    }

    @Operation(summary = "Internal API: Get average rating for a target")
    @GetMapping("/internal/reviews/{targetId}/average")
    public ResponseEntity<ApiResponse<Double>> getAverageRating(@PathVariable String targetId) {
        Double average = reviewService.getAverageRating(targetId);
        
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Average rating retrieved successfully", average)
        );
    }
}
