package com.example.payment.audit;

import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.UUID;

/** Deliberately extends Repository (not JpaRepository): only save + read exist => append-only by construction. */
public interface AuditRepository extends Repository<AuditLog, UUID> {
    AuditLog save(AuditLog log);
    List<AuditLog> findByOrderIdOrderByOccurredAtAsc(UUID orderId);
}