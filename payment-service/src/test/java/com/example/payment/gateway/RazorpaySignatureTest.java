package com.example.payment.gateway.razorpay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RazorpaySignatureTest {

    RazorpayPaymentGateway gw() {
        var p = new RazorpayProperties();

        p.setWebhookSecret("whsec");
        p.setKeySecret("ksec");
        p.setKeyId("k");
        p.setBaseUrl("http://localhost");
        p.setConnectTimeoutMs(100);
        p.setReadTimeoutMs(100);

        return new RazorpayPaymentGateway(p, RestClient.builder());
    }

    @Test
    void validWebhook() {
        String body = "{\"a\":1}";

        assertTrue(gw().verifyWebhookSignature(body, RazorpayPaymentGateway.hmacHex(body, "whsec")));
    }

    @Test
    void tamperedBodyRejected() {
        assertFalse(gw().verifyWebhookSignature("{\"a\":2}", RazorpayPaymentGateway.hmacHex("{\"a\":1}", "whsec")));
    }

    @Test
    void missingSignatureRejected() {
        assertFalse(gw().verifyWebhookSignature("x", null));
    }
}