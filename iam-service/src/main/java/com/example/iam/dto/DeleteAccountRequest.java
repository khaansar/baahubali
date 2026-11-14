package com.example.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DeleteAccountRequest(
        @NotBlank(message = "Confirmation code is required")
        @Pattern(regexp = "[0-9]{6}", message = "Enter the 6-digit code from your email")
        String code
) {
}
