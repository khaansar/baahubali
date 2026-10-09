package com.example.attempt.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.RestClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PaymentClient {

    private final RestClient http;

    public PaymentClient(
        @Value("${payment.service-url}") String url,
        @Value("${internal.token}") String token,
        RestClient.Builder builder
    ) {
        this.http = builder.baseUrl(url).defaultHeader("X-Internal-Token", token).build();
    }

    public boolean hasAccess(UUID userId, UUID testId) {
        try {
            var response = http.post()
                .uri("/internal/entitlements/check")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("userId", userId, "testId", testId))
                .retrieve()
                .body(JsonNode.class);

            return response.path("data").path("allowed").asBoolean(false);

        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to verify access");
        }
    }
}