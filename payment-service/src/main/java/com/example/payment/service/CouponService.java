package com.example.payment.service;

import com.example.payment.entity.Coupon;
import com.example.payment.entity.CouponRedemption;
import com.example.payment.entity.enums.RedemptionStatus;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.repository.CouponRedemptionRepository;
import com.example.payment.repository.CouponRepository;
import com.example.payment.repository.OrderRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;
    private final OrderRepository orders;
    private final CouponUsageJdbc usage;
    private final MeterRegistry metrics;

    public static String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Read-only eligibility check
     * (used for quotes and again at order creation).
     */
    @Transactional(readOnly = true)
    public Coupon validate(String rawCode, UUID userId, UUID productId, long subtotal) {
        String code = normalize(rawCode);

        Coupon c = coupons.findByCode(code).orElseThrow(() -> reject("Coupon is not valid"));

        Instant now = Instant.now();

        if (!"ACTIVE".equals(c.getStatus()) || now.isBefore(c.getStartsAt()) || now.isAfter(c.getExpiresAt())) {
            throw reject("Coupon is not valid");
        }

        if (!c.getProductIds().isEmpty() && !c.getProductIds().contains(productId)) {
            throw reject("Coupon is not valid for this product");
        }

        if (subtotal < c.getMinimumOrderAmount()) {
            throw reject("Order does not meet the minimum amount for this coupon");
        }

        if (c.getUsageLimit() != null && c.getUsedCount() >= c.getUsageLimit()) {
            throw reject("Coupon is not valid");
        }

        if (c.isFirstOrderOnly() && orders.existsPaidByUser(userId)) {
            throw reject("Coupon is for first orders only");
        }

        if (c.getPerUserUsageLimit() != null && redemptions.countActiveByCouponAndUser(c.getId(), userId) >= c.getPerUserUsageLimit()) {
            throw reject("Coupon usage limit reached");
        }

        return c;
    }

    /**
     * Called inside the order-creation transaction.
     * Atomic: either both counters move or the txn rolls back.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public CouponRedemption reserve(Coupon c, UUID userId, UUID orderId, long discount) {
        if (coupons.tryIncrementUsage(c.getId()) != 1) {
            metrics.counter("coupon_rejections_total", "reason", "exhausted").increment();

            throw new PaymentException(ErrorCode.COUPON_EXHAUSTED, "Coupon is no longer available");
        }

        if (!usage.tryIncrementUser(c.getId(), userId, c.getPerUserUsageLimit())) {
            metrics.counter("coupon_rejections_total", "reason", "per_user").increment();

            throw new PaymentException(ErrorCode.COUPON_EXHAUSTED, "Coupon usage limit reached");
        }

        CouponRedemption r = new CouponRedemption();

        r.setCouponId(c.getId());
        r.setUserId(userId);
        r.setOrderId(orderId);
        r.setStatus(RedemptionStatus.RESERVED);
        r.setDiscountAmount(discount);

        metrics.counter("coupon_redemptions_total").increment();

        return redemptions.save(r);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void confirm(UUID orderId) {
        redemptions.findByOrderId(orderId).filter(r -> r.getStatus() == RedemptionStatus.RESERVED).ifPresent(r -> r.setStatus(RedemptionStatus.CONFIRMED));
    }

    /**
     * Failed/expired/cancelled orders give the slot back.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void release(UUID orderId) {
        redemptions.findByOrderId(orderId).filter(r -> r.getStatus() == RedemptionStatus.RESERVED).ifPresent(r -> {
            r.setStatus(RedemptionStatus.RELEASED);
            coupons.decrementUsage(r.getCouponId());
            usage.decrementUser(r.getCouponId(), r.getUserId());
        });
    }

    private PaymentException reject(String m) {
        metrics.counter("coupon_rejections_total", "reason", "invalid").increment();

        return new PaymentException(ErrorCode.COUPON_INVALID, m);
    }
}