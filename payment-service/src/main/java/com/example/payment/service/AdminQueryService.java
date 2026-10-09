package com.example.payment.service;

import com.example.payment.audit.AuditRepository;
import com.example.payment.dto.PageResponse;
import com.example.payment.dto.Views.*;
import com.example.payment.entity.Entitlement;
import com.example.payment.entity.Order;
import com.example.payment.entity.Payment;
import com.example.payment.entity.Refund;
import com.example.payment.entity.enums.EntitlementStatus;
import com.example.payment.entity.enums.OrderStatus;
import com.example.payment.entity.enums.PaymentStatus;
import com.example.payment.entity.enums.RefundStatus;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.repository.EntitlementRepository;
import com.example.payment.repository.OrderRepository;
import com.example.payment.repository.Paging;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.RefundRepository;
import com.example.payment.repository.Specs;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminQueryService {
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final EntitlementRepository entitlements;
    private final AuditRepository audit;
    private final OrderViewAssembler assembler;

    public PageResponse<OrderView> orders(UUID userId, OrderStatus status, String orderNumber, int page, int size) {
        Specification<Order> spec = Specification.<Order>where(Specs.eq("userId", userId))
            .and(Specs.eq("status", status)).and(Specs.eq("orderNumber", orderNumber));
        return PageResponse.of(orders.findAll(spec, Paging.of(page, size)).map(assembler::assemble));
    }

    public OrderDetailView orderDetail(UUID orderId) {
        Order o = orders.findById(orderId).orElseThrow(() -> new PaymentException(ErrorCode.NOT_FOUND, "Order not found"));
        return new OrderDetailView(assembler.assemble(o),
            payments.findByOrderId(orderId).stream().map(PaymentView::of).toList(),
            refunds.findByOrderId(orderId).stream().map(RefundView::of).toList());
    }

    public List<TimelineEntry> timeline(UUID orderId) {
        return audit.findByOrderIdOrderByOccurredAtAsc(orderId).stream().map(TimelineEntry::of).toList();
    }

    public PageResponse<PaymentView> payments(UUID orderId, UUID userId, PaymentStatus status, String providerPaymentId, int page, int size) {
        Specification<Payment> byUser = userId == null ? null : (root, q, cb) -> {
            Subquery<UUID> sq = q.subquery(UUID.class);
            Root<Order> o = sq.from(Order.class);
            sq.select(o.get("id")).where(cb.equal(o.get("userId"), userId));
            return root.get("orderId").in(sq);
        };
        Specification<Payment> spec = Specification.<Payment>where(Specs.eq("orderId", orderId)).and(byUser)
            .and(Specs.eq("status", status)).and(Specs.eq("providerPaymentId", providerPaymentId));
        return PageResponse.of(payments.findAll(spec, Paging.of(page, size)).map(PaymentView::of));
    }

    public PageResponse<RefundView> refunds(UUID orderId, RefundStatus status, int page, int size) {
        Specification<Refund> spec = Specification.<Refund>where(Specs.eq("orderId", orderId)).and(Specs.eq("status", status));
        return PageResponse.of(refunds.findAll(spec, Paging.of(page, size)).map(RefundView::of));
    }

    public PageResponse<EntitlementView> entitlements(UUID userId, EntitlementStatus status, int page, int size) {
        Specification<Entitlement> spec = Specification.<Entitlement>where(Specs.eq("userId", userId)).and(Specs.eq("status", status));
        return PageResponse.of(entitlements.findAll(spec, Paging.of(page, size)).map(EntitlementView::of));
    }

    public RefundableView refundable(UUID paymentId) {
        Payment p = payments.findById(paymentId).orElseThrow(() -> new PaymentException(ErrorCode.NOT_FOUND, "Payment not found"));
        boolean captured = p.getStatus() == PaymentStatus.CAPTURED || p.getStatus() == PaymentStatus.PARTIALLY_REFUNDED;
        long inFlight = refunds.sumByStatuses(paymentId, List.of(RefundStatus.REQUESTED, RefundStatus.PROCESSING));
        long refundable = captured ? Math.max(0, p.getAmount() - p.getRefundedAmount() - inFlight) : 0;
        return new RefundableView(p.getAmount(), p.getRefundedAmount(), inFlight, refundable);
    }
}