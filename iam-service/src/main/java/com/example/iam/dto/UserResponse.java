package com.example.iam.dto;

import com.example.iam.entity.Role;
import com.example.iam.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String targetExam,
        Role role,
        String avatarUrl,
        Boolean isActive,
        Boolean emailVerified,
        Integer testsAttemptedCount,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getTargetExam(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getIsActive(),
                user.getEmailVerified(),
                user.getTestsAttemptedCount(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getDeletedAt()
        );
    }
}