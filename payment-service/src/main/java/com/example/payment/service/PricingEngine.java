package com.example.payment.service;

import com.example.payment.entity.Coupon;
import org.springframework.stereotype.Component;

@Component
public class PricingEngine {

    private static final long BASIS_POINTS = 10_000L;

    /**
     * The coupon may be null.
     * Coupon eligibility must be checked by CouponService before this call.
     */
    public PriceBreakdown calculate(
            String currency,
            long unitPrice,
            int quantity,
            Coupon coupon) {

        if (unitPrice < 0) {
            throw new IllegalArgumentException(
                    "Unit price cannot be negative");
        }

        if (quantity < 1) {
            throw new IllegalArgumentException(
                    "Quantity must be positive");
        }

        long subtotal = Math.multiplyExact(
                unitPrice,
                (long) quantity);

        long discount = coupon == null
                ? 0L
                : discountFor(coupon, subtotal);

        long taxable = Math.subtractExact(subtotal, discount);
        long tax = 0L;

        return new PriceBreakdown(
                currency,
                subtotal,
                discount,
                taxable,
                tax,
                Math.addExact(taxable, tax),
                coupon == null ? null : coupon.getCode(),
                coupon == null ? null : coupon.getId());
    }

    long discountFor(Coupon coupon, long subtotal) {

        long discount = switch (coupon.getDiscountType()) {
            case PERCENTAGE ->
                    Math.multiplyExact(
                            subtotal,
                            coupon.getDiscountValue()) / BASIS_POINTS;

            case FIXED_AMOUNT ->
                    coupon.getDiscountValue();
        };

        if (coupon.getMaximumDiscountAmount() != null) {
            discount = Math.min(
                    discount,
                    coupon.getMaximumDiscountAmount());
        }

        return Math.max(0L, Math.min(discount, subtotal));
    }
}