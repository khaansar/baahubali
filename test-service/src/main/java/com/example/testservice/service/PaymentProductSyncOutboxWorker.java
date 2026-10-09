package com.example.testservice.service;

import com.example.testservice.client.PaymentProductSyncClient;
import com.example.testservice.entity.PaymentProductSyncOutbox;
import com.example.testservice.repository.PaymentProductSyncOutboxRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProductSyncOutboxWorker {

    private final PaymentProductSyncOutboxRepository repository;
    private final PaymentProductSyncOutboxService outbox;
    private final PaymentProductSyncClient client;

    @Scheduled(fixedDelayString = "${payment.sync.fixed-delay-ms:10000}")
    public void processPending() {
        for (PaymentProductSyncOutbox event : repository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc("PENDING", Instant.now())) {
            try {
                client.syncSeries(event.getSeriesId(), event.getTitle(), event.isActive(), event.getBasePriceRupees());
                outbox.markSynced(event.getId());
            } catch (Exception e) {
                outbox.recordFailure(event.getId(), e.getMessage());
                log.error("Payment product sync attempt failed outboxId={} seriesId={} attempts={}", event.getId(), event.getSeriesId(), event.getAttempts() + 1, e);
            }
        }
    }
}