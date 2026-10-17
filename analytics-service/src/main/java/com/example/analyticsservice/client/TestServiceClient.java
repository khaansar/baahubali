package com.example.analyticsservice.client;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TestServiceClient {

    private final RestClient http;

    public TestServiceClient(
            @Value("${internal.services.test-url}") String url,
            @Value("${internal.auth.my-secret}") String secret,
            RestClient.Builder builder) {
        this.http = builder
                .baseUrl(url)
                .defaultHeader("X-Service-Caller", "analytics-service")
                .defaultHeader("X-Service-Auth", secret)
                .build();
    }

    public ServiceResponse<PublishedTestStatus> getPublishedTestStatusBySlug(String slug) {
        return http.get()
                .uri("/internal/mock-tests/slug/{slug}/leaderboard-status", slug)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public record PublishedTestStatus(UUID testId) {
    }
}
