package com.example.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Standard response envelope used by notification-service HTTP APIs. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        ApiError error,
        Map<String, Object> meta
) {

    public static <T> ApiResponse<T> success(
            int status, String message, T data, String version) {
        T responseData = data == null ? emptyObject() : data;
        return new ApiResponse<>(true, status, message, responseData, null,
                metadata(version));
    }

    public static <T> ApiResponse<T> failure(
            int status, String message, String code, Object details, String version) {
        Map<String, Object> meta = new LinkedHashMap<>(metadata(version));
        meta.put("trace_id", "err-" + UUID.randomUUID());
        return new ApiResponse<>(false, status, message, null,
                new ApiError(code, details == null ? List.of() : details), meta);
    }

    @SuppressWarnings("unchecked")
    private static <T> T emptyObject() {
        return (T) Map.of();
    }

    private static Map<String, Object> metadata(String version) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "version", version
        );
    }

    public record ApiError(String code, Object details) {
    }
}
