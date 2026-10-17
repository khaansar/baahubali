package com.example.communityservice.client;

import com.example.communityservice.dto.ApiResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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
                .defaultHeader("X-Service-Caller", "community-service")
                .defaultHeader("X-Service-Auth", secret)
                .build();
    }

    public ApiResponse<Map<String, Boolean>> hasUserAttemptedTest(String userId, String testId) {
        return http.get()
                .uri(builder -> builder.path("/internal/attempts/verify")
                        .queryParam("userId", userId)
                        .queryParam("testId", testId)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
