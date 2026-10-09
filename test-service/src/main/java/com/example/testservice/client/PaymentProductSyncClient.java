package com.example.test.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class PaymentProductSyncClient {
    private final RestClient http;

    public PaymentProductSyncClient(@Value("${payment.service-url}") String url,
                                    @Value("${internal.token}") String token, RestClient.Builder builder) {
        this.http = builder.baseUrl(url).defaultHeader("X-Internal-Token", token).build();
    }

    /** Best-effort, non-blocking for the admin flow: failures are logged and healed by the next save/backfill. */
    public void syncSeries(UUID seriesId, String title, boolean active, BigDecimal basePriceRupees) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("productType", "TEST_SERIES"); body.put("referenceId", seriesId);
            body.put("name", title); body.put("active", active); body.put("currency", "INR");
            body.put("amountMinor", basePriceRupees == null ? null : basePriceRupees.movePointRight(2).longValueExact());
            http.put().uri("/internal/products/sync").contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("payment product sync failed seriesId={}: {}", seriesId, e.getMessage());
        }
    }
}