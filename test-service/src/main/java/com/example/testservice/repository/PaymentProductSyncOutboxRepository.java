package com.example.testservice.repository;

import com.example.testservice.entity.PaymentProductSyncOutbox;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentProductSyncOutboxRepository extends JpaRepository<PaymentProductSyncOutbox, UUID> {
    List<PaymentProductSyncOutbox> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(String status, Instant now);
}