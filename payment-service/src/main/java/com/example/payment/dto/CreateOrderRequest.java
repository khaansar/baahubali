package com.example.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateOrderRequest(
    @NotNull UUID productId,
    @Size(max = 64) String couponCode
) {
}