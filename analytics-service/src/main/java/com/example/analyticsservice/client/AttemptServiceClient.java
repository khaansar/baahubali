package com.example.analyticsservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Writes analytics' completed score to the attempt system of record. */
@Component
public class AttemptServiceClient {

    private final RestClient http;

    public AttemptServiceClient(
            @Value("${internal.services.attempt-url}") String url,
            @Value("${internal.auth.my-secret}") String secret,
            RestClient.Builder builder) {
        this.http = builder
                .baseUrl(url)
                .defaultHeader("X-Service-Caller", "analytics-service")
                .defaultHeader("X-Service-Auth", secret)
                .build();
    }

    public void updateFinalScore(String attemptId, Double finalScore) {
        http.put()
                .uri("/internal/attempts/{attemptId}/final-score", attemptId)
                .body(new FinalScoreUpdateRequest(finalScore))
                .retrieve()
                .toBodilessEntity();
    }

    private record FinalScoreUpdateRequest(Double finalScore) {
    }
}
