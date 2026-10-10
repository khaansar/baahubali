package com.example.payment.repository;

import com.example.payment.entity.Order;
import com.example.payment.entity.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    Page<Order> findByUserId(UUID userId, Pageable p);
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserIdAndStatusIn(UUID userId, Collection<OrderStatus> statuses);

    default boolean existsPaidByUser(UUID userId) {
        return existsByUserIdAndStatusIn(userId,
            List.of(OrderStatus.PAID, OrderStatus.FULFILLED, OrderStatus.PARTIALLY_REFUNDED, OrderStatus.REFUNDED));
    }

    @Query(
        value = "SELECT * FROM orders WHERE id = :id FOR UPDATE",
        nativeQuery = true
    )
    Optional<Order> lockById(@Param("id") UUID id);

    @Query("SELECT o.id FROM Order o WHERE o.status IN :st AND o.expiresAt < :now")
    List<UUID> findExpiredIds(@Param("st") Collection<OrderStatus> st, @Param("now") Instant now, Pageable page);
}