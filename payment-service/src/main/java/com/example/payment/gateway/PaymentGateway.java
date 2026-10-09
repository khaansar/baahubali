package com.example.payment.gateway;

import java.util.List;
import java.util.Map;

public interface PaymentGateway {

    String name();

    ProviderOrder createOrder(String receipt, long amountMinor, String currency, Map<String, String> notes);

    boolean verifyCheckoutSignature(String providerOrderId, String providerPaymentId, String signature);

    boolean verifyWebhookSignature(String rawBody, String signature);

    ProviderPayment fetchPayment(String providerPaymentId);

    List<ProviderPayment> fetchPaymentsForOrder(String providerOrderId);

    ProviderRefund refundPayment(String providerPaymentId, long amountMinor, String idempotencyKey, Map<String, String> notes);

    ProviderRefund fetchRefund(String providerRefundId);
}