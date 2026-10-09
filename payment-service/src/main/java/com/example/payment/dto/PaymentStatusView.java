package com.example.payment.dto;

import java.util.UUID;

public record PaymentStatusView(
    UUID paymentId,
    UUID orderId,
    String paymentStatus,
    String orderStatus,
    String resultState
) {
}