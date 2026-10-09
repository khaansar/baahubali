package com.example.payment.service;

import com.example.payment.audit.AuditService;
import com.example.payment.entity.enums.OrderStatus;
import com.example.payment.repository.OrderRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryService {

    private final OrderRepository orders;
    private final CouponService coupons;
    private final AuditService audit;
    private final TransactionTemplate tx;
    private final MeterRegistry metrics;

    @Scheduled(fixedDelay = 60_000)
    public void expire() {
        for (UUID id : orders.findExpiredIds(List.of(OrderStatus.CREATED, OrderStatus.PAYMENT_PENDING), Instant.now(), PageRequest.of(0, 200))) {
            tx.executeWithoutResult(s -> orders.lockById(id).ifPresent(o -> {
                if (o.getStatus() != OrderStatus.CREATED && o.getStatus() != OrderStatus.PAYMENT_PENDING) {
                    return;
                }

                o.transitionTo(OrderStatus.EXPIRED);
                coupons.release(id);
                audit.record("SYSTEM", null, "ORDER_EXPIRED", "ORDER", id, id, null);
                metrics.counter("orders_expired_total").increment();
            }));
        }
    }
}