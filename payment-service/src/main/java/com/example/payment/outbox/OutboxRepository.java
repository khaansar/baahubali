package com.example.payment.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(value = "SELECT * FROM outbox_events WHERE status='PENDING' ORDER BY created_at LIMIT :n FOR UPDATE SKIP LOCKED",
           nativeQuery = true)
    List<OutboxEvent> claimPending(@Param("n") int n);
    long countByEventType(String eventType);
}