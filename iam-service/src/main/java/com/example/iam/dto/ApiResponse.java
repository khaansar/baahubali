package com.example.iam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.example.iam.config.ApiVersion;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;

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

    public static <T> ApiResponse<java.util.List<T>> paginated(
            int status, String message, Page<T> page) {
        Map<String, Object> pagination = Map.of(
                "total_records", page.getTotalElements(),
                "current_page", page.getNumber() + 1,
                "total_pages", page.getTotalPages(),
                "per_page", page.getSize(),
                "has_next", page.hasNext(),
                "has_previous", page.hasPrevious()
        );
        Map<String, Object> meta = Map.of(
                "pagination", pagination,
                "timestamp", Instant.now().toString()
        );
        return new ApiResponse<>(true, status, message, page.getContent(), null, meta);
    }

    @SuppressWarnings("unchecked")
    private static <T> T emptyObject() {
        return (T) Map.of();
    }

    public static ApiResponse<Void> error(
            int status, String message, String code, java.util.List<ApiErrorDetail> details) {
        return error(status, message, code, details, "err-" + UUID.randomUUID());
    }

    public static ApiResponse<Void> error(
            int status, String message, String code, java.util.List<ApiErrorDetail> details, String traceId) {
        ApiErrorResponse error = new ApiErrorResponse(code, details);
        Map<String, Object> meta = Map.of(
                "timestamp", Instant.now().toString(),
                "trace_id", traceId
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
