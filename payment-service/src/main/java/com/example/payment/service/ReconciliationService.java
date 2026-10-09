package com.example.payment.service;

import com.example.payment.config.PaymentProperties;
import com.example.payment.entity.Payment;
import com.example.payment.entity.Refund;
import com.example.payment.enums.PaymentStatus;
import com.example.payment.enums.RefundStatus;
import com.example.payment.gateway.PaymentGateway;
import com.example.payment.gateway.ProviderPayment;
import com.example.payment.gateway.ProviderRefund;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.RefundRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final PaymentGateway gateway;
    private final WebhookProcessor processor;
    private final RefundEntitlementPolicy refundPolicy;
    private final TransactionTemplate tx;
    private final PaymentProperties props;
    private final AuditService audit;
    private final MeterRegistry metrics;

    @Scheduled(fixedDelayString = "${payment.reconciliation.fixed-delay-ms}")
    public void reconcilePayments() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(props.getReconciliation().getPendingOlderThanMinutes()));

        for (Payment p : payments.findStale(List.of(PaymentStatus.CREATED, PaymentStatus.AUTHORIZED), cutoff, PageRequest.of(0, 100))) {
            try {
                // Look at ALL provider payments for this
                // provider order because the user may have retried.
                for (ProviderPayment pp : gateway.fetchPaymentsForOrder(p.getProviderOrderId())) {
                    if ("captured".equals(pp.status())) {
                        metrics.counter("reconciliation_mismatches_total").increment();

                        tx.executeWithoutResult(s -> {
                            Payment locked = payments.lockById(p.getId()).orElseThrow();

                            processor.applyCapture(locked, pp.id(), pp.amount(), pp.currency(), pp.method(), "RECONCILIATION");

                            audit.record("SYSTEM", null, "RECONCILIATION_REPAIRED", "PAYMENT", p.getId(), p.getOrderId(), "provider=captured, local=" + p.getStatus());
                        });

                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("reconcile payment failed paymentId={}: {}", p.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelayString = "${payment.reconciliation.fixed-delay-ms}")
    public void reconcileRefunds() {
        for (Refund r : refunds.findStuck(List.of(RefundStatus.REQUESTED, RefundStatus.PROCESSING), Instant.now().minus(Duration.ofMinutes(5)), PageRequest.of(0, 100))) {
            try {
                if (r.getProviderRefundId() == null) {
                    // REQUESTED without provider id:
                    // needs admin retry.
                    continue;
                }

                ProviderRefund pr = gateway.fetchRefund(r.getProviderRefundId());

                if ("processed".equals(pr.status())) {
                    tx.executeWithoutResult(s -> refundPolicy.settle(refunds.lockById(r.getId()).orElseThrow(), true, "RECONCILIATION"));
                } else if ("failed".equals(pr.status())) {
                    tx.executeWithoutResult(s -> refundPolicy.settle(refunds.lockById(r.getId()).orElseThrow(), false, "RECONCILIATION"));
                }
            } catch (Exception e) {
                log.warn("reconcile refund failed refundId={}", r.getId());
            }
        }
    }
}