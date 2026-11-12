package com.example.payment.service;

import com.example.payment.audit.AuditService;
import com.example.payment.repository.CouponRedemptionRepository;
import com.example.payment.repository.CouponRepository;
import com.example.payment.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.example.payment.entity.Coupon;
import com.example.payment.exception.PaymentException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCouponServiceTest {
    @Mock CouponRepository coupons; @Mock CouponRedemptionRepository redemptions;
    @Mock ProductRepository products; @Mock AuditService audit;

    @SuppressWarnings("unchecked")
    @Test void listWithoutSearchTermDoesNotFailAndPassesNonNullSpec() {
        when(coupons.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        var service = new AdminCouponService(coupons, redemptions, products, audit);

        assertEquals(0, service.list(null, 0, 20).content().size());
        assertEquals(0, service.list("  ", 0, 20).content().size());
        verify(coupons, org.mockito.Mockito.times(2)).findAll((Specification) notNull(), any(Pageable.class));
    }

    @Test void getReturnsCouponOrNotFound() {
        var service = new AdminCouponService(coupons, redemptions, products, audit);
        Coupon c = new Coupon(); UUID id = UUID.randomUUID(); c.setId(id); c.setCode("WELCOME20");
        c.setDiscountType(com.example.payment.entity.enums.DiscountType.PERCENTAGE);
        when(coupons.findById(id)).thenReturn(Optional.of(c));
        assertEquals("WELCOME20", service.get(id).code());
        assertThrows(PaymentException.class, () -> service.get(UUID.randomUUID()));
    }
}
