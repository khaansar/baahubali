package com.example.payment.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, UUID> {
    Optional<IdempotencyKey> findByUserIdAndOperationAndIdemKey(UUID userId, String operation, String idemKey);
    @Modifying @Query("DELETE FROM IdempotencyKey k WHERE k.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}