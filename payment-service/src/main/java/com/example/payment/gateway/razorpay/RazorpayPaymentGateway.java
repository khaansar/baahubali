package com.example.payment.gateway.razorpay;

import com.example.payment.config.RazorpayProperties;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.gateway.PaymentGateway;
import com.example.payment.gateway.ProviderOrder;
import com.example.payment.gateway.ProviderPayment;
import com.example.payment.gateway.ProviderRefund;
import tools.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Component
@Slf4j
public class RazorpayPaymentGateway implements PaymentGateway {

    private final RestClient http;
    private final RazorpayProperties props;

    public RazorpayPaymentGateway(RazorpayProperties props, RestClient.Builder builder) {
        this.props = props;
        var rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(props.getConnectTimeoutMs());
        rf.setReadTimeout(props.getReadTimeoutMs());
        this.http = builder.baseUrl(props.getBaseUrl())
            .requestFactory(rf)
            .defaultHeaders(h -> h.setBasicAuth(props.getKeyId(), props.getKeySecret()))
            .build();
    }

    @Override
    public String name() {
        return "RAZORPAY";
    }

    @Override
    public ProviderOrder createOrder(String receipt, long amount, String currency, Map<String, String> notes) {
        JsonNode n = call(() -> http.post()
            .uri("/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("amount", amount, "currency", currency, "receipt", receipt, "notes", notes))
            .retrieve()
            .body(JsonNode.class));
        return new ProviderOrder(n.path("id").asText(), n.path("amount").asLong(), n.path("currency").asText(), n.path("status").asText());
    }

    @Override
    public boolean verifyCheckoutSignature(String orderId, String paymentId, String sig) {
        return sig != null && constantTimeEquals(hmacHex(orderId + "|" + paymentId, props.getKeySecret()), sig);
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, String sig) {
        return sig != null && constantTimeEquals(hmacHex(rawBody, props.getWebhookSecret()), sig);
    }

    @Override
    public ProviderPayment fetchPayment(String id) {
        return toPayment(call(() -> http.get().uri("/payments/{id}", id).retrieve().body(JsonNode.class)));
    }

    @Override
    public List<ProviderPayment> fetchPaymentsForOrder(String orderId) {
        JsonNode n = call(() -> http.get().uri("/orders/{id}/payments", orderId).retrieve().body(JsonNode.class));
        List<ProviderPayment> out = new ArrayList<>();
        n.path("items").forEach(i -> out.add(toPayment(i)));
        return out;
    }

    @Override
    public ProviderRefund refundPayment(String paymentId, long amount, String idemKey, Map<String, String> notes) {
        JsonNode n = call(() -> http.post()
            .uri("/payments/{id}/refund", paymentId)
            .header("X-Refund-Idempotency", idemKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("amount", amount, "notes", notes))
            .retrieve()
            .body(JsonNode.class));
        return toRefund(n);
    }

    @Override
    public ProviderRefund fetchRefund(String id) {
        return toRefund(call(() -> http.get().uri("/refunds/{id}", id).retrieve().body(JsonNode.class)));
    }

    private <T> T call(Supplier<T> s) {
        try {
            T r = s.get();
            if (r == null) throw new PaymentException(ErrorCode.PROVIDER_ERROR, "Empty provider response");
            return r;
        } catch (ResourceAccessException e) {
            throw new PaymentException(ErrorCode.PROVIDER_UNAVAILABLE, "Payment provider timed out/unreachable");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 409) {
                throw new PaymentException(ErrorCode.PROVIDER_UNAVAILABLE, "Payment provider request is still processing; outcome is uncertain");
            }
            log.warn("Razorpay 4xx status={}", e.getStatusCode());
            throw new PaymentException(ErrorCode.PROVIDER_ERROR, "Payment provider rejected the request");
        } catch (HttpServerErrorException e) {
            throw new PaymentException(ErrorCode.PROVIDER_UNAVAILABLE, "Payment provider error");
        }
    }

    private ProviderPayment toPayment(JsonNode n) {
        return new ProviderPayment(n.path("id").asText(), n.path("order_id").asText(null), n.path("amount").asLong(), n.path("currency").asText(), n.path("status").asText(), n.path("method").asText(null), n.path("error_code").asText(null), n.path("error_description").asText(null), n.path("amount_refunded").asLong());
    }

    private ProviderRefund toRefund(JsonNode n) {
        return new ProviderRefund(n.path("id").asText(), n.path("payment_id").asText(), n.path("amount").asLong(), n.path("status").asText());
    }

    static String hmacHex(String data, String secret) {
        try {
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(m.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static boolean constantTimeEquals(String a, String b) {
        return a != null && b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}