package com.example.payment.service;

import com.example.payment.audit.AuditService;
import com.example.payment.dto.PageResponse;
import com.example.payment.dto.Views.OrderView;
import com.example.payment.entity.Order;
import com.example.payment.entity.Payment;
import com.example.payment.entity.enums.OrderStatus;
import com.example.payment.entity.enums.PaymentStatus;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.repository.OrderRepository;
import com.example.payment.repository.Paging;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final OrderViewAssembler assembler;
    private final CouponService coupons;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<OrderView> list(UUID userId, int page, int size) {
        return PageResponse.of(orders.findByUserId(userId, Paging.of(page, size)).map(assembler::assemble));
    }

    @Transactional(readOnly = true)
    public OrderView get(UUID userId, UUID orderId) {
        return assembler.assemble(orders.findByIdAndUserId(orderId, userId).orElseThrow(OrderService::notFound));
    }

    @Transactional
    public OrderView cancel(UUID userId, UUID orderId) {
        orders.findByIdAndUserId(orderId, userId).orElseThrow(OrderService::notFound);   // ownership check
        Order o = orders.lockById(orderId).orElseThrow(OrderService::notFound);
        if (o.getStatus() == OrderStatus.PAID || o.getStatus() == OrderStatus.FULFILLED)
            throw new PaymentException(ErrorCode.ORDER_ALREADY_PAID, "Order is already paid");
        o.transitionTo(OrderStatus.CANCELLED);               // throws INVALID_STATE_TRANSITION otherwise
        for (Payment p : payments.findByOrderId(orderId))
            if (p.getStatus() == PaymentStatus.CREATED) p.setStatus(PaymentStatus.CANCELLED);
        coupons.release(orderId);
        audit.record("USER", userId.toString(), "ORDER_CANCELLED", "ORDER", orderId, orderId, null);
        return assembler.assemble(o);
    }
    private static PaymentException notFound() { return new PaymentException(ErrorCode.NOT_FOUND, "Order not found"); }
}