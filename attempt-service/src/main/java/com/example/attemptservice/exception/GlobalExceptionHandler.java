package com.example.attemptservice.exception;

import com.example.attemptservice.dto.ApiResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AttemptNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(AttemptNotFoundException ex) {
        return failure(HttpStatus.NOT_FOUND, "ATTEMPT_NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        return failure(ex.getStatusCode(), status == null ? "REQUEST_FAILED" : status.name(),
                ex.getReason() == null ? "Request failed" : ex.getReason(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "issue",
                        error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()))
                .toList();
        return failure(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Validation failed for the submitted input", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return failure(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Request body is missing or malformed", null);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException ex) {
        return failure(HttpStatus.BAD_REQUEST, "MISSING_REQUIRED_HEADER", "A required request header is missing",
                List.of(Map.of("field", ex.getHeaderName(), "issue", "Header is required")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return failure(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "A request value has an invalid format",
                List.of(Map.of("field", ex.getName(), "issue", "Value has an invalid format")));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return failure(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "HTTP method is not supported for this endpoint", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        return failure(HttpStatus.NOT_FOUND, "NOT_FOUND", "Endpoint not found", null);
    }

    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<ApiResponse<Void>> handleTestNotFound(FeignException.NotFound ex) {
        return failure(HttpStatus.NOT_FOUND, "TEST_NOT_FOUND",
                "The requested test does not exist or is not published", null);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleTestServiceFailure(FeignException ex) {
        log.error("Test service request failed with HTTP status {}", ex.status(), ex);
        return failure(HttpStatus.BAD_GATEWAY, "TEST_SERVICE_ERROR",
                "Test service could not complete the request", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        String traceId = "err-" + UUID.randomUUID();
        log.error("Unhandled request failure service=attempt-service trace_id={} method={} path={}",
                traceId, request.getMethod(), request.getRequestURI(), ex);
        ApiResponse<Void> response = ApiResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "The server could not complete the request. Contact support with the trace ID.",
                new ApiResponse.ApiError("INTERNAL_ERROR", List.of()), traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ResponseEntity<ApiResponse<Void>> failure(HttpStatusCode status, String code, String message, Object details) {
        ApiResponse<Void> response = ApiResponse.failure(status.value(), message,
                new ApiResponse.ApiError(code, details));
        return ResponseEntity.status(status).body(response);
    }
}
