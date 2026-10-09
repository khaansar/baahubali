package com.example.payment.dto;

import com.example.payment.audit.AuditLog;
import com.example.payment.entity.*;
import com.example.payment.entity.enums.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Views {
    private Views() {}

    public record OrderItemView(UUID productId, ProductType productType, UUID productReferenceId, String name,
                                int quantity, long unitPrice, long discount, long tax, long finalAmount) {
        public static OrderItemView of(OrderItem i) {
            return new OrderItemView(i.getProductId(), i.getProductType(), i.getProductReferenceId(), i.getProductNameSnapshot(),
                i.getQuantity(), i.getUnitPriceAmount(), i.getDiscountAmount(), i.getTaxAmount(), i.getFinalAmount());
        }
    }
    public record OrderView(UUID id, UUID userId, String orderNumber, OrderStatus status, String currency, long subtotal,
                            long discount, long tax, long total, String couponCode, Instant paidAt, Instant createdAt,
                            Instant expiresAt, List<OrderItemView> items, long refundedAmount) {
        public static OrderView of(Order o, List<OrderItem> items, long refunded) {
            return new OrderView(o.getId(), o.getUserId(), o.getOrderNumber(), o.getStatus(), o.getCurrency(),
                o.getSubtotalAmount(), o.getDiscountAmount(), o.getTaxAmount(), o.getTotalAmount(), o.getCouponCode(),
                o.getPaidAt(), o.getCreatedAt(), o.getExpiresAt(), items.stream().map(OrderItemView::of).toList(), refunded);
        }
    }
    public record PaymentView(UUID id, UUID orderId, String provider, String providerOrderId, String providerPaymentId,
                              long amount, String currency, PaymentStatus status, String method, String failureCode,
                              String failureReason, long refundedAmount, Instant capturedAt, Instant createdAt) {
        public static PaymentView of(Payment p) {
            return new PaymentView(p.getId(), p.getOrderId(), p.getProvider(), p.getProviderOrderId(), p.getProviderPaymentId(),
                p.getAmount(), p.getCurrency(), p.getStatus(), p.getMethod(), p.getFailureCode(), p.getFailureReason(),
                p.getRefundedAmount(), p.getCapturedAt(), p.getCreatedAt());
        }
    }
    public record PaymentStatusView(UUID paymentId, UUID orderId, PaymentStatus paymentStatus, OrderStatus orderStatus,
                                    String resultState, String failureReason) {}
    public record RefundView(UUID id, UUID paymentId, UUID orderId, long amount, String currency, String reason,
                             RefundStatus status, String providerRefundId, UUID requestedBy, Instant requestedAt, Instant processedAt) {
        public static RefundView of(Refund r) {
            return new RefundView(r.getId(), r.getPaymentId(), r.getOrderId(), r.getAmount(), r.getCurrency(), r.getReason(),
                r.getStatus(), r.getProviderRefundId(), r.getRequestedBy(), r.getRequestedAt(), r.getProcessedAt());
        }
    }
    public record EntitlementView(UUID id, UUID userId, ProductType productType, UUID productReferenceId, EntitlementSource source,
                                  UUID sourceOrderId, EntitlementStatus status, Instant grantedAt, Instant expiresAt, Instant revokedAt) {
        public static EntitlementView of(Entitlement e) {
            return new EntitlementView(e.getId(), e.getUserId(), e.getProductType(), e.getProductReferenceId(), e.getSource(),
                e.getSourceOrderId(), e.getStatus(), e.getGrantedAt(), e.getExpiresAt(), e.getRevokedAt());
        }
    }
    public record CouponView(UUID id, String code, String description, DiscountType discountType, long discountValue,
                             long minimumOrderAmount, Long maximumDiscountAmount, Instant startsAt, Instant expiresAt,
                             Integer usageLimit, Integer perUserUsageLimit, int usedCount, boolean stackable,
                             boolean firstOrderOnly, String status, Set<UUID> productIds) {
        public static CouponView of(Coupon c) {
            return new CouponView(c.getId(), c.getCode(), c.getDescription(), c.getDiscountType(), c.getDiscountValue(),
                c.getMinimumOrderAmount(), c.getMaximumDiscountAmount(), c.getStartsAt(), c.getExpiresAt(), c.getUsageLimit(),
                c.getPerUserUsageLimit(), c.getUsedCount(), c.isStackable(), c.isFirstOrderOnly(), c.getStatus(), Set.copyOf(c.getProductIds()));
        }
    }
    public record RedemptionView(UUID id, UUID userId, UUID orderId, RedemptionStatus status, long discountAmount, Instant createdAt) {
        public static RedemptionView of(CouponRedemption r) {
            return new RedemptionView(r.getId(), r.getUserId(), r.getOrderId(), r.getStatus(), r.getDiscountAmount(), r.getCreatedAt());
        }
    }
    public record TimelineEntry(Instant at, String actorType, String actorId, String action, String aggregateType,
                                String aggregateId, String reason) {
        public static TimelineEntry of(AuditLog a) {
            return new TimelineEntry(a.getOccurredAt(), a.getActorType(), a.getActorId(), a.getAction(),
                a.getAggregateType(), a.getAggregateId(), a.getReason());
        }
    }
    public record OrderDetailView(OrderView order, List<PaymentView> payments, List<RefundView> refunds) {}
    public record RefundableView(long captured, long refunded, long inFlight, long refundable) {}
    public record ProductView(UUID id, ProductType type, UUID referenceId, String name, Long currentPriceMinor, String currency) {}
}