package com.example.testservice.service;

import com.example.testservice.entity.PaymentProductSyncOutbox;
import com.example.testservice.repository.PaymentProductSyncOutboxRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentProductSyncOutboxService {

    private final PaymentProductSyncOutboxRepository repository;

    @Transactional
    public void enqueue(UUID seriesId, String title, boolean active, BigDecimal basePriceRupees) {
        PaymentProductSyncOutbox event = new PaymentProductSyncOutbox();
        event.setSeriesId(seriesId);
        event.setTitle(title);
        event.setActive(active);
        event.setBasePriceRupees(basePriceRupees);
        event.setStatus("PENDING");
        event.setAttempts(0);
        event.setNextAttemptAt(Instant.now());
        repository.save(event);
    }

    @Transactional
    public void markSynced(UUID id) {
        PaymentProductSyncOutbox event = repository.findById(id).orElseThrow();
        event.setStatus("SYNCED");
        event.setLastError(null);
    }

    @Transactional
    public void recordFailure(UUID id, String message) {
        PaymentProductSyncOutbox event = repository.findById(id).orElseThrow();
        int attempts = event.getAttempts() + 1;
        event.setAttempts(attempts);
        event.setLastError(message == null ? "Unknown payment sync error" : message.substring(0, Math.min(1000, message.length())));
        event.setStatus(attempts >= 10 ? "FAILED" : "PENDING");
        event.setNextAttemptAt(Instant.now().plusSeconds(Math.min(3600L, 10L << Math.min(attempts, 8))));
    }
}