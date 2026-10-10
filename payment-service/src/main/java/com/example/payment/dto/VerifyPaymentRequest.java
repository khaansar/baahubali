package com.example.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyPaymentRequest(
    @NotBlank String providerPaymentId,
    @NotBlank String signature
) {
}