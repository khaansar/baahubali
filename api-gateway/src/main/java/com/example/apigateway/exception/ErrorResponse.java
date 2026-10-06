package com.example.apigateway.exception;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ErrorResponse(
        boolean success,
        int status,
        String message,
        Error error,
        Meta meta) {

    public static ErrorResponse of(GatewayErrorCode errorCode) {
        return of(errorCode, "err-" + UUID.randomUUID());
    }

    public static ErrorResponse of(GatewayErrorCode errorCode, String traceId) {
        return new ErrorResponse(
                false,
                errorCode.getStatus().value(),
                errorCode.getClientMessage(),
                new Error(errorCode.name(), List.of()),
                new Meta(Instant.now().toString(), traceId));
    }

    public record Error(String code, List<?> details) {}

    public record Meta(String timestamp, String trace_id) {}
}
