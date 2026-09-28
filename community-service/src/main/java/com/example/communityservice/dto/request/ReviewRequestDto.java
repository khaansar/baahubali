package com.example.communityservice.dto.request;

import com.example.communityservice.entity.Review;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewRequestDto(
        @NotBlank(message = "Target ID cannot be blank") 
        String targetId,
        
        @NotNull(message = "Target Type is required") 
        Review.TargetType targetType,
        
        @NotNull(message = "Rating is required") 
        @Min(value = 1, message = "Rating must be at least 1") 
        @Max(value = 5, message = "Rating cannot exceed 5") 
        Integer rating,
        
        String comment
) {}