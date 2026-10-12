package com.example.analyticsservice.client;

public record ServiceResponse<T>(
        boolean success,
        int status,
        String message,
        T data
) {
}
