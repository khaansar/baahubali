package com.example.attemptservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        ApiError error,
        Map<String, Object> meta) {

    private static final String API_VERSION = "1.2.0";

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return success(status, message, data, Map.of());
    }

    public static <T> ApiResponse<T> success(int status, String message, T data, Map<String, ?> additionalMeta) {
        T responseData = data == null ? emptyObject() : data;
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("timestamp", Instant.now().toString());
        meta.put("version", API_VERSION);
        if (additionalMeta != null) {
            meta.putAll(additionalMeta);
        }
        return new ApiResponse<>(true, status, message, responseData, null,
                meta);
    }

    @SuppressWarnings("unchecked")
    private static <T> T emptyObject() {
        return (T) Map.of();
    }

    public static <T> ApiResponse<T> paginated(int status, String message, T data,
                                                long totalRecords, int currentPage, int perPage) {
        int totalPages = perPage <= 0 ? 0 : (int) Math.ceil((double) totalRecords / perPage);
        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("total_records", totalRecords);
        pagination.put("current_page", currentPage);
        pagination.put("total_pages", totalPages);
        pagination.put("per_page", perPage);
        pagination.put("has_next", currentPage < totalPages);
        pagination.put("has_previous", currentPage > 1);
        return success(status, message, data, Map.of("pagination", pagination));
    }

    public static ApiResponse<Void> failure(int status, String message, ApiError error) {
        return failure(status, message, error, "err-" + UUID.randomUUID());
    }

    public static ApiResponse<Void> failure(int status, String message, ApiError error, String traceId) {
        ApiError normalizedError = error == null
                ? new ApiError("INTERNAL_ERROR", List.of())
                : new ApiError(error.code(), error.details() == null ? List.of() : error.details());
        return new ApiResponse<>(false, status, message, null, normalizedError,
                Map.of("timestamp", Instant.now().toString(),
                        "trace_id", traceId == null || traceId.isBlank() ? "err-" + UUID.randomUUID() : traceId));
    }

    public record ApiError(String code, Object details) { }
}
