package com.example.testservice.client;

import com.example.testservice.dto.ApiResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CommunityServiceClient {

    private final RestClient http;

    public CommunityServiceClient(
            @Value("${internal.services.community-url}") String url,
            @Value("${internal.auth.my-secret}") String secret,
            RestClient.Builder builder) {
        this.http = builder
                .baseUrl(url)
                .defaultHeader("X-Service-Caller", "test-service")
                .defaultHeader("X-Service-Auth", secret)
                .build();
    }

    public ApiResponse<Map<String, Double>> getAverageRating(String targetId) {
        return http.get()
                .uri("/internal/reviews/{targetId}/average", targetId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
