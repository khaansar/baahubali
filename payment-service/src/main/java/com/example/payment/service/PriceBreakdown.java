package com.example.payment.service;

import java.util.UUID;


public record PriceBreakdown(
        String currency,
        long subtotal,
        long discount,
        long taxable,
        long tax,
        long total,
        String couponCode,
        UUID couponId) {
}