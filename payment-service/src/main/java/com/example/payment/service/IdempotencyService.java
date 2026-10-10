package com.example.payment.service;

import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.idempotency.IdempotencyKey;
import com.example.payment.idempotency.IdempotencyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository repository;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;

    public record BeginResult(IdempotencyKey row, boolean replay) {}

    public BeginResult begin(UUID userId, String operation, String key, Object request) {
        if (key == null || key.isBlank()) throw new PaymentException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED, "Idempotency-Key is required");
        if (key.length() > 80) throw new PaymentException(ErrorCode.VALIDATION_FAILED, "Idempotency-Key exceeds 80 characters");

        String hash = hash(request);
        try {
            return tx.execute(status -> {
                Optional<IdempotencyKey> existing = repository.findByUserIdAndOperationAndIdemKey(userId, operation, key);
                if (existing.isPresent()) return existingResult(existing.get(), hash);

                IdempotencyKey row = new IdempotencyKey();
                row.setUserId(userId);
                row.setOperation(operation);
                row.setIdemKey(key);
                row.setRequestHash(hash);
                row.setStatus("IN_PROGRESS");
                row.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
                repository.saveAndFlush(row);
                return new BeginResult(row, false);
            });
        } catch (DataIntegrityViolationException race) {
            return tx.execute(status -> {
                IdempotencyKey existing = repository.findByUserIdAndOperationAndIdemKey(userId, operation, key)
                    .orElseThrow(() -> race);
                return existingResult(existing, hash);
            });
        }
    }

    public <T> T replay(IdempotencyKey row, Class<T> responseType) {
        if (!"COMPLETED".equals(row.getStatus()) || row.getResponseBody() == null) {
            throw new PaymentException(ErrorCode.IDEMPOTENCY_IN_PROGRESS, "A request with this Idempotency-Key is still in progress or did not complete");
        }
        try {
            return mapper.readValue(row.getResponseBody(), responseType);
        } catch (JsonProcessingException e) {
            throw new PaymentException(ErrorCode.INTERNAL_ERROR, "Stored idempotent response could not be read");
        }
    }

    public void complete(IdempotencyKey row, int responseStatus, Object response) {
        String body;
        try {
            body = mapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new PaymentException(ErrorCode.INTERNAL_ERROR, "Response could not be stored for idempotent replay");
        }

        tx.executeWithoutResult(status -> {
            IdempotencyKey current = repository.findById(row.getId()).orElseThrow();
            current.setStatus("COMPLETED");
            current.setResponseStatus(responseStatus);
            current.setResponseBody(body);
            repository.save(current);
        });
    }

    public void fail(IdempotencyKey row) {
        if (row == null) return;
        tx.executeWithoutResult(status -> repository.findById(row.getId()).ifPresent(current -> {
            if (!"COMPLETED".equals(current.getStatus())) {
                current.setStatus("FAILED");
                repository.save(current);
            }
        }));
    }

    private BeginResult existingResult(IdempotencyKey row, String requestHash) {
        if (!MessageDigest.isEqual(row.getRequestHash().getBytes(StandardCharsets.UTF_8), requestHash.getBytes(StandardCharsets.UTF_8))) {
            throw new PaymentException(ErrorCode.IDEMPOTENCY_CONFLICT, "Idempotency-Key was already used with a different request");
        }
        if ("COMPLETED".equals(row.getStatus())) return new BeginResult(row, true);
        if ("IN_PROGRESS".equals(row.getStatus())) {
            throw new PaymentException(ErrorCode.IDEMPOTENCY_IN_PROGRESS, "A request with this Idempotency-Key is already in progress");
        }
        throw new PaymentException(ErrorCode.IDEMPOTENCY_CONFLICT, "The previous request failed; use a new Idempotency-Key to start another attempt");
    }

    private String hash(Object request) {
        try {
            byte[] bytes = mapper.writeValueAsBytes(request);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new PaymentException(ErrorCode.INTERNAL_ERROR, "Request could not be hashed for idempotency");
        }
    }
}