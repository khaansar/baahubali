package com.example.payment.service;

import com.example.payment.dto.Views.OrderView;
import com.example.payment.entity.Order;
import com.example.payment.entity.Payment;
import com.example.payment.repository.OrderItemRepository;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderViewAssembler {
    private final OrderItemRepository items;
    private final PaymentRepository payments;

    public OrderView assemble(Order o) {
        long refunded = payments.findByOrderId(o.getId()).stream().mapToLong(Payment::getRefundedAmount).sum();
        return OrderView.of(o, items.findByOrderId(o.getId()), refunded);
    }
}