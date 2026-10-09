package com.example.payment.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {
    private static final int MAX_ATTEMPTS = 20;
    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate tx;

    /** Claim + send + mark in ONE txn so SKIP LOCKED really prevents multi-instance double-sends. */
    @Scheduled(fixedDelay = 1000)
    public void relay() {
        tx.executeWithoutResult(s -> {
            for (OutboxEvent e : outbox.claimPending(100)) {
                try {
                    kafka.send(e.getTopic(), e.getAggregateId(), e.getPayload()).get(5, TimeUnit.SECONDS);
                    e.setStatus("PUBLISHED");
                    e.setPublishedAt(Instant.now());
                } catch (Exception ex) {
                    if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                    e.setAttempts(e.getAttempts() + 1);
                    if (e.getAttempts() >= MAX_ATTEMPTS) e.setStatus("DEAD");   // alert on DEAD rows
                    log.warn("outbox publish failed eventId={} attempts={}", e.getId(), e.getAttempts());
                    break;
                }
            }
        });
    }
}