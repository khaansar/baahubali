package com.example.payment.repository;

import com.example.payment.entity.Payment;
import com.example.payment.entity.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {
    List<Payment> findByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.provider = :prov AND p.providerOrderId = :oid")
    Optional<Payment> lockByProviderOrderId(@Param("prov") String prov, @Param("oid") String oid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> lockById(@Param("id") UUID id);

    @Query("SELECT p FROM Payment p WHERE p.status IN :st AND p.createdAt < :cutoff ORDER BY p.createdAt")
    List<Payment> findStale(@Param("st") Collection<PaymentStatus> st, @Param("cutoff") Instant cutoff, Pageable page);
}