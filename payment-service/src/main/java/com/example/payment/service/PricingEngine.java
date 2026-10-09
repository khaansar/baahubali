package com.example.payment.service;

import com.example.payment.entity.Coupon;
import org.springframework.stereotype.Component;

import java.util.UUID;

public record PriceBreakdown(String currency, long subtotal, long discount, long taxable, long tax, long total, String couponCode, UUID couponId) {}

@Component
public class PricingEngine {

    private static final long BPS = 10_000L;

    /**
     * coupon may be null.
     * Coupon eligibility is checked by CouponService before this call.
     */
    public PriceBreakdown calculate(String currency, long unitPrice, int qty, Coupon coupon) {
        long subtotal = Math.multiplyExact(unitPrice, (long) qty);

        long discount = coupon == null ? 0 : discountFor(coupon, subtotal);

        long taxable = subtotal - discount;

        long tax = 0; 

        return new PriceBreakdown(currency, subtotal, discount, taxable, tax, taxable + tax, coupon == null ? null : coupon.getCode(), coupon == null ? null : coupon.getId());
    }

    long discountFor(Coupon c, long subtotal) {
        long d = switch (c.getDiscountType()) {
            case PERCENTAGE -> Math.multiplyExact(subtotal, c.getDiscountValue()) / BPS;
            case FIXED_AMOUNT -> c.getDiscountValue();
        };

        if (c.getMaximumDiscountAmount() != null) {
            d = Math.min(d, c.getMaximumDiscountAmount());
        }

        return Math.max(0, Math.min(d, subtotal));
    }
}