package com.example.iam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        String email,

        @NotBlank(message = "Verification code is required")
        @Pattern(regexp = "[0-9]{6}", message = "Enter the 6-digit code from your email")
        String code
) {
}
