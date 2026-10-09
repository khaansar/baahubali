package com.example.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.payment.entity.Coupon;
import com.example.payment.enums.DiscountType;
import org.junit.jupiter.api.Test;

class PricingEngineTest {

    PricingEngine e = new PricingEngine();

    Coupon pct(long bps, Long max) {
        Coupon c = new Coupon();

        c.setDiscountType(DiscountType.PERCENTAGE);
        c.setDiscountValue(bps);
        c.setMaximumDiscountAmount(max);
        c.setCode("X");

        return c;
    }

    Coupon fix(long v) {
        Coupon c = new Coupon();

        c.setDiscountType(DiscountType.FIXED_AMOUNT);
        c.setDiscountValue(v);
        c.setCode("X");

        return c;
    }

    @Test
    void noCoupon() {
        var b = e.calculate("INR", 49900, 1, null);

        assertEquals(49900, b.total());
        assertEquals(0, b.discount());
    }

    @Test
    void percentage() {
        assertEquals(39920, e.calculate("INR", 49900, 1, pct(2000, null)).total());
    }

    @Test
    void percentageCapped() {
        assertEquals(10000, e.calculate("INR", 49900, 1, pct(5000, 10000L)).discount());
    }

    @Test
    void fixed() {
        assertEquals(39900, e.calculate("INR", 49900, 1, fix(10000)).total());
    }

    @Test
    void fixedNeverExceedsSubtotal() {
        var b = e.calculate("INR", 5000, 1, fix(10000));

        assertEquals(0, b.total());
        assertEquals(5000, b.discount());
    }

    @Test
    void floorRounding() {
        assertEquals(33, e.calculate("INR", 333, 1, pct(1000, null)).discount());
    }
}