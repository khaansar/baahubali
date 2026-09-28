package com.example.iam.dto;

public record UserProfileDto(
        String id,
        String displayName,
        String avatarUrl
) {}