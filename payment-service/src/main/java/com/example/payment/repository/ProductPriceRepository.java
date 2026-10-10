package com.example.payment.repository;

import com.example.payment.entity.ProductPrice;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductPriceRepository extends JpaRepository<ProductPrice, UUID> {

    @Query("""
        SELECT p
        FROM ProductPrice p
        WHERE p.productId = :pid
          AND p.currency = :cur
          AND p.status = 'ACTIVE'
          AND p.effectiveFrom <= :now
          AND (
              p.effectiveUntil IS NULL
              OR p.effectiveUntil > :now
          )
        ORDER BY p.effectiveFrom DESC
        LIMIT 1
        """)
    Optional<ProductPrice> findActive(@Param("pid") UUID pid, @Param("cur") String cur, @Param("now") Instant now);
}