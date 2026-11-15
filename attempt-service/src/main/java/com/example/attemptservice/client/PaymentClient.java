package com.example.attemptservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PaymentClient {

    private final RestClient http;

    public PaymentClient(
            @Value("${payment.service-url}") String url,
            @Value("${internal.auth.my-secret}") String secret,
            RestClient.Builder builder) {

        this.http = builder
                .baseUrl(url)
                .defaultHeader("X-Service-Caller", "attempt-service")
                .defaultHeader("X-Service-Auth", secret)
                .build();
    }

    public boolean hasAccess(UUID userId, UUID testId) {
        try {
            AccessCheckResponse response = http.post()
                    .uri("/internal/entitlements/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("userId", userId, "testId", testId))
                    .retrieve()
                    .body(AccessCheckResponse.class);

            return response != null
                    && response.data() != null
                    && Boolean.TRUE.equals(response.data().allowed());

        } catch (RestClientException | HttpMessageConversionException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to verify access");
        }
    }

    /** Payment service's response envelope; only `data.allowed` is needed. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccessCheckResponse(AccessCheck data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccessCheck(Boolean allowed) {
    }
}
