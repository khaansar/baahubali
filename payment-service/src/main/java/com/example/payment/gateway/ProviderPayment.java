package com.example.payment.gateway;

public record ProviderPayment(String id, String orderId, long amount, String currency, String status, String method, String errorCode, String errorDescription, long amountRefunded) {}