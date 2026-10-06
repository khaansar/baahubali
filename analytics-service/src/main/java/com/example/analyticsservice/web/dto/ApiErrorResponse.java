package com.example.analyticsservice.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiErrorResponse(
        boolean success,
        int status,
        String message,
        ApiError error,
        Map<String, Object> meta) {

    public static ApiErrorResponse failure(int status, String message, String code,
                                           Object details, String traceId) {
        Object safeDetails = details == null ? List.of() : details;
        return new ApiErrorResponse(false, status, message, new ApiError(code, safeDetails),
                Map.of("timestamp", Instant.now().toString(), "trace_id", traceId));
    }

    public record ApiError(String code, Object details) { }
}
