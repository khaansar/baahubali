package com.example.payment.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class IdempotencyCleanup {
    private final IdempotencyRepository repo;
    private final TransactionTemplate tx;

    @Scheduled(cron = "0 15 3 * * *")
    public void purge() { tx.executeWithoutResult(s -> repo.deleteExpired(Instant.now())); }
}