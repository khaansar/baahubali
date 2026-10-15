package com.example.iam.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        @Size(max = 100, message = "Target exam must not exceed 100 characters")
        String targetExam,

        @Size(max = 512, message = "Avatar URL must not exceed 512 characters")
        String avatarUrl,

        Boolean removeAvatar
) {}
