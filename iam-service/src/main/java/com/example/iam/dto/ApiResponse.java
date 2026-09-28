package com.example.iam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.example.iam.config.ApiVersion;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        ApiErrorResponse error,
        Map<String, Object> meta
) {
    public static <T> ApiResponse<T> success(int status, String message, T data) {
        T responseData = data == null ? emptyObject() : data;
        return new ApiResponse<>(true, status, message, responseData, null, defaultMeta());
    }

    @SuppressWarnings("unchecked")
    private static <T> T emptyObject() {
        return (T) Map.of();
    }

    public static ApiResponse<Void> error(
            int status, String message, String code, java.util.List<ApiErrorDetail> details) {
        ApiErrorResponse error = new ApiErrorResponse(code, details);
        Map<String, Object> meta = Map.of(
                "timestamp", Instant.now().toString(),
                "trace_id", UUID.randomUUID().toString()
        );
        return new ApiResponse<>(false, status, message, null, error, meta);
    }

    private static Map<String, Object> defaultMeta() {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "version", ApiVersion.current()
        );
    }
}
