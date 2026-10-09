package com.example.payment.repository;

import com.example.payment.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    Optional<WebhookEvent> findByProviderAndProviderEventId(String provider, String eventId);

    @Modifying @Query("UPDATE WebhookEvent w SET w.processed = true, w.processedAt = :at WHERE w.id = :id")
    int markProcessed(@Param("id") UUID id, @Param("at") Instant at);

    @Modifying @Query("UPDATE WebhookEvent w SET w.attempts = w.attempts + 1, w.lastError = :err WHERE w.id = :id")
    int recordFailure(@Param("id") UUID id, @Param("err") String err);
}