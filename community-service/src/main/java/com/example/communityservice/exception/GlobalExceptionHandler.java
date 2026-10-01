package com.example.communityservice.exception;

import com.example.communityservice.dto.ApiErrorDetail;
import com.example.communityservice.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<ApiErrorDetail> details = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> new ApiErrorDetail(
                        ((FieldError) error).getField(),
                        error.getDefaultMessage()))
                .collect(Collectors.toList());

        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY,
                "Validation failed for the submitted input", "INVALID_INPUT", details);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String code = switch (status) {
            case BAD_REQUEST -> "INVALID_INPUT";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case NOT_FOUND -> "NOT_FOUND";
            case CONFLICT -> "CONFLICT";
            default -> "HTTP_ERROR";
        };
        return buildErrorResponse(status, ex.getReason(), code, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralExceptions(Exception ex, HttpServletRequest request) {
        String traceId = "err-" + UUID.randomUUID();
        log.error("Unhandled request failure service=community-service trace_id={} method={} path={}",
                traceId, request.getMethod(), request.getRequestURI(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "The server could not complete the request. Contact support with the trace ID.",
                "INTERNAL_SERVER_ERROR", List.of(), traceId);
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status, String message, String errorCode, List<ApiErrorDetail> details) {
        return buildErrorResponse(status, message, errorCode, details, "err-" + UUID.randomUUID());
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status, String message,
                                                                 String errorCode, List<ApiErrorDetail> details,
                                                                 String traceId) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .success(false)
                .status(status.value())
                .message(message)
                .error(ApiErrorResponse.ErrorData.builder()
                        .code(errorCode)
                        .details(details)
                        .build())
                .meta(ApiErrorResponse.MetaData.builder()
                        .timestamp(Instant.now().toString())
                        .traceId(traceId)
                        .build())
                .build();

        return new ResponseEntity<>(response, status);
    }
}
