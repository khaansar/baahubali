package com.example.iam.dto;

public record NotificationContactResponse(
        String userId,
        String email,
        String firstName,
        boolean emailVerified
) {
}