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

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
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

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation Failed", "VALIDATION_ERROR", details);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        return buildErrorResponse(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason(), "HTTP_ERROR", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralExceptions(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", "INTERNAL_SERVER_ERROR", List.of());
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status, String message, String errorCode, List<ApiErrorDetail> details) {
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
                        .traceId(UUID.randomUUID().toString())
                        .build())
                .build();

        return new ResponseEntity<>(response, status);
    }
}