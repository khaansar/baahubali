package com.example.analyticsservice.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        Map<String, Object> meta) {

    public static <T> ApiResponse<T> success(int status, String message, T data, String version) {
        return new ApiResponse<>(true, status, message, data,
                Map.of("timestamp", Instant.now().toString(), "version", version));
    }
}
