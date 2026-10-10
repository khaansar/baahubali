package com.example.payment.exception;

import com.example.payment.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(PaymentException.class)
    ResponseEntity<ApiResponse<Void>> domain(PaymentException e) {
        return ResponseEntity.status(e.getCode().http).body(ApiResponse.error(e.getCode().name(), e.getMessage()));
    }
    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ApiResponse<Void>> header(MissingRequestHeaderException e) {
        boolean idem = "Idempotency-Key".equalsIgnoreCase(e.getHeaderName());
        return ResponseEntity.badRequest().body(ApiResponse.error(idem ? "IDEMPOTENCY_KEY_REQUIRED" : "VALIDATION_FAILED",
            idem ? "Idempotency-Key header is required" : "Missing header " + e.getHeaderName()));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
        MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<ApiResponse<Void>> validation(Exception e) {
        log.warn("Payment API request validation failed: {}", e.getMessage(), e);
        return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_FAILED", "Invalid request"));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> integrity(DataIntegrityViolationException e) {
        log.warn("integrity violation: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(409).body(ApiResponse.error("CONFLICT", "Resource already exists or is in use"));
    }
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiResponse<Void>> locking(Exception e) {
        return ResponseEntity.status(409).body(ApiResponse.error("CONCURRENT_UPDATE", "Please retry"));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unknown(Exception e) {
        log.error("Unhandled", e);
        return ResponseEntity.status(500).body(ApiResponse.error("INTERNAL_ERROR", "Unexpected error"));
    }
}