package com.example.testservice.exception;

import com.example.testservice.dto.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(ValidationException ex) {
        List<?> details = ex.getErrors() == null ? Collections.emptyList() : ex.getErrors().stream()
                .map(issue -> new ValidationDetail(null, issue)).toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(400, ex.getMessage(), "VALIDATION_ERROR", details));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiErrorResponse> handleBindingValidationException(BindException ex) {
        List<ValidationDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        return badRequest("Validation failed for the submitted input", "VALIDATION_ERROR", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<ValidationDetail> details = ex.getConstraintViolations().stream()
                .map(violation -> new ValidationDetail(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return badRequest("Validation failed for the submitted input", "VALIDATION_ERROR", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return badRequest("Request body is malformed or contains an invalid value", "INVALID_INPUT",
                Collections.emptyList());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return badRequest("A required request parameter is missing", "INVALID_INPUT",
                List.of(new ValidationDetail(ex.getParameterName(), "Parameter is required")));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        return badRequest("A required request header is missing", "INVALID_INPUT",
                List.of(new ValidationDetail(ex.getHeaderName(), "Header is required")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return badRequest("A request value has an invalid format", "INVALID_INPUT",
                List.of(new ValidationDetail(ex.getName(), "Value has an invalid format")));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(new ApiErrorResponse(405,
                "HTTP method is not supported for this endpoint", "METHOD_NOT_ALLOWED", Collections.emptyList()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(404,
                "Endpoint not found", "NOT_FOUND", Collections.emptyList()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(404, ex.getMessage(), "NOT_FOUND", Collections.emptyList()));
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflictException(ResourceConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(409, ex.getMessage(), "CONFLICT", Collections.emptyList()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        HttpStatus resolvedStatus = HttpStatus.resolve(status);
        String code = resolvedStatus != null ? resolvedStatus.name() : "REQUEST_ERROR";
        return ResponseEntity.status(ex.getStatusCode()).body(new ApiErrorResponse(status,
                ex.getReason() != null ? ex.getReason() : "Request failed", code, Collections.emptyList()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String traceId = "err-" + UUID.randomUUID();
        log.error("Unhandled request failure service=test-service trace_id={} method={} path={}",
                traceId, request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(500,
                "The server could not complete the request. Contact support with the trace ID.",
                "INTERNAL_ERROR", Collections.emptyList(), traceId));
    }

    private record ValidationDetail(String field, String issue) {}

    private ResponseEntity<ApiErrorResponse> badRequest(String message, String code, List<?> details) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(400, message, code, details));
    }
}
