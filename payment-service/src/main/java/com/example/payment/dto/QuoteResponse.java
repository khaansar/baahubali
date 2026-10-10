package com.example.payment.dto;

import com.example.payment.service.PriceBreakdown;
import java.time.Instant;
import java.util.UUID;

public record QuoteResponse(
    UUID productId,
    String productName,
    PriceBreakdown price,
    Instant quoteExpiresAt
) {
}