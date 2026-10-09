package com.example.payment.service;

import com.example.payment.audit.AuditService;
import com.example.payment.dto.AdminRequests.CouponRequest;
import com.example.payment.dto.PageResponse;
import com.example.payment.dto.Views.CouponView;
import com.example.payment.dto.Views.RedemptionView;
import com.example.payment.entity.Coupon;
import com.example.payment.entity.enums.DiscountType;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.repository.CouponRedemptionRepository;
import com.example.payment.repository.CouponRepository;
import com.example.payment.repository.Paging;
import com.example.payment.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCouponService {
    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;
    private final ProductRepository products;
    private final AuditService audit;

    @Transactional
    public CouponView create(UUID adminId, CouponRequest r) {
        String code = CouponService.normalize(r.code());
        if (coupons.findByCode(code).isPresent()) throw new PaymentException(ErrorCode.VALIDATION_FAILED, "Coupon code already exists");
        Coupon c = new Coupon();
        c.setCode(code);
        apply(c, r);
        coupons.saveAndFlush(c);
        audit.record("ADMIN", adminId.toString(), "COUPON_CREATED", "COUPON", c.getId(), null, "code=" + code);
        return CouponView.of(c);
    }

    /** Code is immutable. Changes only affect future orders; existing redemptions/orders are untouched. */
    @Transactional
    public CouponView update(UUID adminId, UUID id, CouponRequest r) {
        Coupon c = find(id);
        if (!c.getCode().equals(CouponService.normalize(r.code())))
            throw new PaymentException(ErrorCode.VALIDATION_FAILED, "Coupon code cannot be changed");
        apply(c, r);
        coupons.saveAndFlush(c);
        audit.record("ADMIN", adminId.toString(), "COUPON_UPDATED", "COUPON", id, null, "code=" + c.getCode());
        return CouponView.of(c);
    }

    @Transactional
    public CouponView setEnabled(UUID adminId, UUID id, boolean enabled) {
        Coupon c = find(id);
        c.setStatus(enabled ? "ACTIVE" : "DISABLED");        // never deleted: redemption history stays
        coupons.saveAndFlush(c);
        audit.record("ADMIN", adminId.toString(), enabled ? "COUPON_ENABLED" : "COUPON_DISABLED", "COUPON", id, null, "code=" + c.getCode());
        return CouponView.of(c);
    }

    @Transactional(readOnly = true)
    public PageResponse<CouponView> list(String q, int page, int size) {
        Specification<Coupon> spec = (q == null || q.isBlank()) ? null
            : (root, query, cb) -> cb.like(root.get("code"), q.trim().toUpperCase(Locale.ROOT) + "%");
        return PageResponse.of(coupons.findAll(Specification.where(spec), Paging.of(page, size)).map(CouponView::of));
    }

    @Transactional(readOnly = true)
    public PageResponse<RedemptionView> redemptions(UUID id, int page, int size) {
        find(id);
        return PageResponse.of(redemptions.findByCouponId(id, Paging.of(page, size)).map(RedemptionView::of));
    }

    private void apply(Coupon c, CouponRequest r) {
        if (!r.expiresAt().isAfter(r.startsAt())) throw bad("expiresAt must be after startsAt");
        if (r.discountType() == DiscountType.PERCENTAGE && r.discountValue() > 10_000) throw bad("Percentage is in basis points (max 10000)");
        if (r.usageLimit() != null && r.usageLimit() < c.getUsedCount()) throw bad("usageLimit is below current usage");
        Set<UUID> ids = r.productIds() == null ? Set.of() : r.productIds();
        if (!ids.isEmpty() && products.findAllById(ids).size() != ids.size()) throw bad("Unknown productId in applicability list");
        c.setDescription(r.description()); c.setDiscountType(r.discountType()); c.setDiscountValue(r.discountValue());
        c.setMinimumOrderAmount(r.minimumOrderAmount()); c.setMaximumDiscountAmount(r.maximumDiscountAmount());
        c.setStartsAt(r.startsAt()); c.setExpiresAt(r.expiresAt());
        c.setUsageLimit(r.usageLimit()); c.setPerUserUsageLimit(r.perUserUsageLimit());
        c.setStackable(r.stackable()); c.setFirstOrderOnly(r.firstOrderOnly());
        c.setProductIds(new HashSet<>(ids));
    }
    private Coupon find(UUID id) { return coupons.findById(id).orElseThrow(() -> new PaymentException(ErrorCode.NOT_FOUND, "Coupon not found")); }
    private static PaymentException bad(String m) { return new PaymentException(ErrorCode.VALIDATION_FAILED, m); }
}