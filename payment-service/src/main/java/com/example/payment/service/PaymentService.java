package com.example.payment.service;

import com.example.payment.dto.VerifyPaymentRequest;
import com.example.payment.dto.Views.PaymentStatusView;
import com.example.payment.entity.Order;
import com.example.payment.entity.Payment;
import com.example.payment.entity.enums.OrderStatus;
import com.example.payment.entity.enums.PaymentStatus;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.gateway.PaymentGateway;
import com.example.payment.gateway.ProviderPayment;
import com.example.payment.repository.OrderRepository;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final PaymentGateway gateway;
    private final WebhookProcessor processor;
    private final TransactionTemplate tx;

    public PaymentStatusView get(UUID userId, UUID paymentId) {
        Payment pay = payments.findById(paymentId).orElseThrow(PaymentService::notFound);
        Order order = orders.findById(pay.getOrderId()).orElseThrow(PaymentService::notFound);
        if (!order.getUserId().equals(userId)) throw notFound();
        return view(pay, order);
    }

    /** A HINT, never authority: the checkout signature only triggers a provider fetch; state comes from the provider. */
    public PaymentStatusView verify(UUID userId, UUID paymentId, VerifyPaymentRequest req) {
        PaymentStatusView current = get(userId, paymentId);
        Payment pay = payments.findById(paymentId).orElseThrow(PaymentService::notFound);
        if (pay.getStatus() == PaymentStatus.CAPTURED || pay.getStatus() == PaymentStatus.PARTIALLY_REFUNDED
            || pay.getStatus() == PaymentStatus.REFUNDED) return current;

        if (!gateway.verifyCheckoutSignature(pay.getProviderOrderId(), req.providerPaymentId(), req.signature()))
            throw new PaymentException(ErrorCode.WEBHOOK_SIGNATURE_INVALID, "Payment signature invalid");

        ProviderPayment pp = gateway.fetchPayment(req.providerPaymentId());
        if (!pay.getProviderOrderId().equals(pp.orderId()))
            throw new PaymentException(ErrorCode.PROVIDER_ERROR, "Provider order mismatch");
        if ("captured".equals(pp.status())) {
            tx.executeWithoutResult(s -> {
                Payment locked = payments.lockByProviderOrderId(gateway.name(), pay.getProviderOrderId()).orElseThrow();
                processor.applyCapture(locked, pp.id(), pp.amount(), pp.currency(), pp.method(), "USER_VERIFY");
            });
        }
        return get(userId, paymentId);
    }

    static PaymentStatusView view(Payment p, Order o) {
        String state = switch (o.getStatus()) {
            case PAID, FULFILLED, PARTIALLY_REFUNDED, REFUNDED -> "SUCCESS";
            case FAILED, EXPIRED, CANCELLED -> "FAILED";
            case PAYMENT_REVIEW -> "PROCESSING";
            default -> "PROCESSING";
        };
        return new PaymentStatusView(p.getId(), o.getId(), p.getStatus(), o.getStatus(), state, p.getFailureReason());
    }

    private static PaymentException notFound() { return new PaymentException(ErrorCode.NOT_FOUND, "Payment not found"); }
}