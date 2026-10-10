package com.example.payment.gateway;

public record ProviderOrder(String id, long amount, String currency, String status) {}