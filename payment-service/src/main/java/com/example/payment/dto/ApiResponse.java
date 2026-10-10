package com.example.payment.dto;

import java.time.Instant;

public record ApiResponse<T>(boolean success, T data, ErrorBody error, Instant timestamp) {
    public record ErrorBody(String code, String message) {}
    public static <T> ApiResponse<T> ok(T d) { return new ApiResponse<>(true, d, null, Instant.now()); }
    public static <T> ApiResponse<T> error(String c, String m) { return new ApiResponse<>(false, null, new ErrorBody(c, m), Instant.now()); }
}