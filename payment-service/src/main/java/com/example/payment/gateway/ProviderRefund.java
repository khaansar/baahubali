package com.example.payment.gateway;

public record ProviderRefund(String id, String paymentId, long amount, String status) {}