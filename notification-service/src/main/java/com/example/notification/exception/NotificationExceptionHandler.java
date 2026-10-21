package com.example.notification.exception;

import com.example.notification.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

/** Converts framework and application errors into the standard API envelope. */
@RestControllerAdvice
@Slf4j
public class NotificationExceptionHandler {

    @Value("${app.api.version}")
    private String apiVersion;

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return failure(status, status.name(),
                exception.getReason() == null ? "Request failed" : exception.getReason(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        List<Map<String, String>> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "issue", error.getDefaultMessage() == null
                                ? "Invalid value" : error.getDefaultMessage()))
                .toList();
        return failure(HttpStatus.BAD_REQUEST, "INVALID_INPUT",
                "Validation failed for the submitted input", details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(Exception exception) {
        return failure(HttpStatus.BAD_REQUEST, "INVALID_INPUT",
                "A request value has an invalid format", null);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException exception) {
        return failure(HttpStatus.BAD_REQUEST, "MISSING_REQUIRED_HEADER",
                "A required request header is missing",
                List.of(Map.of("field", exception.getHeaderName(), "issue", "Header is required")));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return failure(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "HTTP method is not supported for this endpoint", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException exception) {
        return failure(HttpStatus.NOT_FOUND, "NOT_FOUND", "Endpoint not found", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception exception, HttpServletRequest request) {
        log.error("Unhandled notification request failure method={} path={}",
                request.getMethod(), request.getRequestURI(), exception);
        return failure(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "The server could not complete the request", null);
    }

    private ResponseEntity<ApiResponse<Void>> failure(
            HttpStatus status, String code, String message, Object details) {
        return ResponseEntity.status(status).body(ApiResponse.failure(
                status.value(), message, code, details, apiVersion));
    }
}
