package com.example.testservice.client;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AttemptServiceClient {

    private final RestClient http;

    public AttemptServiceClient(
            @Value("${internal.services.attempt-url}") String url,
            @Value("${internal.auth.my-secret}") String secret,
            RestClient.Builder builder) {
        this.http = builder
                .baseUrl(url)
                .defaultHeader("X-Service-Caller", "test-service")
                .defaultHeader("X-Service-Auth", secret)
                .build();
    }

    public boolean hasActiveAttempts(UUID testId) {
        Boolean response = http.get()
                .uri("/internal/attempts/active-exists/{testId}", testId)
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(response);
    }
}
