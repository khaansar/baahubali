package com.example.payment.repository;

import com.example.payment.entity.Refund;
import com.example.payment.entity.enums.RefundStatus;
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

public interface RefundRepository extends JpaRepository<Refund, UUID>, JpaSpecificationExecutor<Refund> {
    List<Refund> findByOrderId(UUID orderId);
    Optional<Refund> findByPaymentIdAndIdempotencyKey(UUID paymentId, String key);

    @Query("SELECT COALESCE(SUM(r.amount),0) FROM Refund r WHERE r.paymentId = :p AND r.status IN :st")
    long sumByStatuses(@Param("p") UUID paymentId, @Param("st") Collection<RefundStatus> st);

    default long sumActiveAmount(UUID paymentId) {
        return sumByStatuses(paymentId, List.of(RefundStatus.REQUESTED, RefundStatus.PROCESSING, RefundStatus.SUCCEEDED));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("SELECT r FROM Refund r WHERE r.id = :id")
    Optional<Refund> lockById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("SELECT r FROM Refund r WHERE r.providerRefundId = :id")
    Optional<Refund> lockByProviderRefundId(@Param("id") String id);

    @Query("SELECT r FROM Refund r WHERE r.status IN :st AND r.updatedAt < :cutoff ORDER BY r.updatedAt")
    List<Refund> findStuck(@Param("st") Collection<RefundStatus> st, @Param("cutoff") Instant cutoff, Pageable page);
}