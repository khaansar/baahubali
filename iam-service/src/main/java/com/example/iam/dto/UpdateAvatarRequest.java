package com.example.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAvatarRequest(
        @NotBlank(message = "Avatar URL is required")
        @Size(max = 512, message = "Avatar URL must not exceed 512 characters")
        String avatarUrl
) {
}