package com.example.analyticsservice.web.exception;

import com.example.analyticsservice.web.dto.ApiErrorResponse;
import com.example.analyticsservice.web.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class AnalyticsExceptionHandler {

    private final String apiVersion;

    public AnalyticsExceptionHandler(@Value("${app.api.version:1.2.0}") String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatus(ResponseStatusException exception) {
        HttpStatusCode status = exception.getStatusCode();
        String message = hasText(exception.getReason()) ? exception.getReason() : "Request failed";

        if (status.value() == HttpStatus.ACCEPTED.value()) {
            return ResponseEntity.status(status).body(ApiResponse.success(
                    status.value(), message, Map.of("status", "PROCESSING"), apiVersion));
        }

        return failure(status, responseCode(status, message), message, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<Map<String, String>> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "issue",
                        error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()))
                .toList();
        return failure(HttpStatus.BAD_REQUEST, "INVALID_INPUT",
                "Validation failed for the submitted input", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<Map<String, String>> details = exception.getConstraintViolations().stream()
                .map(violation -> Map.of("field", violation.getPropertyPath().toString(),
                        "issue", violation.getMessage()))
                .toList();
        return failure(HttpStatus.BAD_REQUEST, "INVALID_INPUT",
                "Validation failed for the submitted input", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return failure(HttpStatus.BAD_REQUEST, "INVALID_INPUT",
                "Request body is missing or malformed", List.of());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingHeader(MissingRequestHeaderException exception) {
        return failure(HttpStatus.BAD_REQUEST, "MISSING_REQUIRED_HEADER",
                "A required request header is missing",
                List.of(Map.of("field", exception.getHeaderName(), "issue", "Header is required")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return failure(HttpStatus.BAD_REQUEST, "INVALID_INPUT",
                "A request value has an invalid format",
                List.of(Map.of("field", exception.getName(), "issue", "Value has an invalid format")));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return failure(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "HTTP method is not supported for this endpoint", List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEndpointNotFound(NoResourceFoundException exception) {
        return failure(HttpStatus.NOT_FOUND, "ENDPOINT_NOT_FOUND", "Endpoint not found", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        String traceId = newTraceId();
        log.error("Unhandled request failure service=analytics-service trace_id={} method={} path={}",
                traceId, request.getMethod(), request.getRequestURI(), exception);
        return failure(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "The server could not complete the request. Contact support with the trace ID.", List.of(), traceId);
    }

    private ResponseEntity<ApiErrorResponse> failure(HttpStatusCode status, String code,
                                                      String message, Object details) {
        return failure(status, code, message, details, newTraceId());
    }

    private ResponseEntity<ApiErrorResponse> failure(HttpStatusCode status, String code,
                                                      String message, Object details, String traceId) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.failure(status.value(), message, code, details, traceId));
    }

    private String responseCode(HttpStatusCode status, String message) {
        if (status.value() == HttpStatus.NOT_FOUND.value()) {
            return message.toLowerCase().contains("report") ? "REPORT_NOT_FOUND" : "NOT_FOUND";
        }
        if (status.value() == HttpStatus.FORBIDDEN.value()) {
            return "ACCESS_DENIED";
        }
        if (status.value() == HttpStatus.UNAUTHORIZED.value()) {
            return "UNAUTHORIZED";
        }
        if (status.value() == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
            return "REPORT_GENERATION_FAILED";
        }
        return HttpStatus.resolve(status.value()) == null ? "REQUEST_FAILED" : HttpStatus.valueOf(status.value()).name();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String newTraceId() {
        return "err-" + UUID.randomUUID();
    }
}
