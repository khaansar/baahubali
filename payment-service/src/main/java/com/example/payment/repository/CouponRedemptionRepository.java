package com.example.payment.repository;

import com.example.payment.entity.CouponRedemption;
import com.example.payment.entity.enums.RedemptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID> {
    Optional<CouponRedemption> findByOrderId(UUID orderId);
    Page<CouponRedemption> findByCouponId(UUID couponId, Pageable p);
    long countByCouponIdAndUserIdAndStatusIn(UUID couponId, UUID userId, Collection<RedemptionStatus> st);

    default long countActiveByCouponAndUser(UUID couponId, UUID userId) {
        return countByCouponIdAndUserIdAndStatusIn(couponId, userId, List.of(RedemptionStatus.RESERVED, RedemptionStatus.CONFIRMED));
    }
}