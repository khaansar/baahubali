package com.example.payment.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateOrderResponse(
    UUID orderId,
    String orderNumber,
    UUID paymentId,
    String provider,
    String providerOrderId,
    String providerKeyId,
    long amount,
    String currency,
    Instant expiresAt
) {
}