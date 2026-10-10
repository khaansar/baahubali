package com.example.testservice.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class PaymentProductSyncClient {

    private final RestClient http;

    public PaymentProductSyncClient(
            @Value("${internal.services.payment-url}") String url,
            @Value("${internal.auth.my-secret}") String secret,
            RestClient.Builder builder) {

        this.http = builder
                .baseUrl(url)
                .defaultHeader("X-Service-Caller", "test-service")
                .defaultHeader("X-Service-Auth", secret)
                .build();
    }

    public void syncSeries(
            UUID seriesId,
            String title,
            boolean active,
            BigDecimal basePriceRupees) {

        Long amountMinor = null;

        if (basePriceRupees != null) {
            if (basePriceRupees.signum() < 0) {
                throw new IllegalArgumentException(
                        "Test-series price cannot be negative");
            }

            amountMinor = basePriceRupees
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("productType", "TEST_SERIES");
        body.put("referenceId", seriesId);
        body.put("name", title);
        body.put("active", active);
        body.put("currency", "INR");
        body.put("amountMinor", amountMinor);

        http.put()
                .uri("/internal/products/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}