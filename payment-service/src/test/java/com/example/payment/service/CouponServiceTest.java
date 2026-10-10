package com.example.payment.service;

import com.example.payment.entity.Coupon;
import com.example.payment.entity.enums.DiscountType;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.repository.CouponRedemptionRepository;
import com.example.payment.repository.CouponRepository;
import com.example.payment.repository.OrderRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CouponServiceTest {
    @Mock CouponRepository coupons; @Mock CouponRedemptionRepository redemptions; @Mock OrderRepository orders; @Mock CouponUsageJdbc usage;
    CouponService service;
    UUID user = UUID.randomUUID(), product = UUID.randomUUID();

    @BeforeEach void setUp() { service = new CouponService(coupons, redemptions, orders, usage, new SimpleMeterRegistry()); }

    Coupon coupon() {
        Coupon c = new Coupon(); c.setCode("WELCOME20"); c.setDiscountType(DiscountType.PERCENTAGE); c.setDiscountValue(2000);
        c.setStartsAt(Instant.now().minusSeconds(60)); c.setExpiresAt(Instant.now().plusSeconds(3600));
        when(coupons.findByCode("WELCOME20")).thenReturn(Optional.of(c));
        return c;
    }
    void rejects(String code, long subtotal) {
        var ex = assertThrows(PaymentException.class, () -> service.validate(code, user, product, subtotal));
        assertEquals(ErrorCode.COUPON_INVALID, ex.getCode());
    }

    @Test void validCouponAcceptedAndCodeNormalized() { Coupon c = coupon(); assertEquals(c, service.validate("  welcome20 ", user, product, 49900)); }
    @Test void unknownCodeRejected() { when(coupons.findByCode("NOPE")).thenReturn(Optional.empty()); rejects("nope", 100); }
    @Test void disabledRejected() { coupon().setStatus("DISABLED"); rejects("welcome20", 49900); }
    @Test void expiredRejected() { coupon().setExpiresAt(Instant.now().minusSeconds(1)); rejects("welcome20", 49900); }
    @Test void notYetActiveRejected() { coupon().setStartsAt(Instant.now().plusSeconds(60)); rejects("welcome20", 49900); }
    @Test void wrongProductRejected() { coupon().setProductIds(Set.of(UUID.randomUUID())); rejects("welcome20", 49900); }
    @Test void belowMinimumRejected() { coupon().setMinimumOrderAmount(50000); rejects("welcome20", 49900); }
    @Test void globalLimitReachedRejected() { Coupon c = coupon(); c.setUsageLimit(5); c.setUsedCount(5); rejects("welcome20", 49900); }
    @Test void firstOrderOnlyRejectedForReturningCustomer() {
        coupon().setFirstOrderOnly(true); when(orders.existsPaidByUser(user)).thenReturn(true); rejects("welcome20", 49900);
    }
    @Test void perUserLimitRejected() {
        Coupon c = coupon(); c.setPerUserUsageLimit(1);
        when(redemptions.countActiveByCouponAndUser(c.getId(), user)).thenReturn(1L); rejects("welcome20", 49900);
    }
}