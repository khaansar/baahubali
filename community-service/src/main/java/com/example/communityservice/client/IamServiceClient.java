package com.example.communityservice.client;

import com.example.communityservice.dto.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class IamServiceClient {

    private final RestClient http;

    public IamServiceClient(
            @Value("${internal.services.iam-url}") String url,
            @Value("${internal.auth.my-secret}") String secret,
            RestClient.Builder builder) {
        this.http = builder
                .baseUrl(url)
                .defaultHeader("X-Service-Caller", "community-service")
                .defaultHeader("X-Service-Auth", secret)
                .build();
    }

    public ApiResponse<Map<String, UserProfileDto>> getUsersBatch(List<String> userIds) {
        return http.post()
                .uri("/internal/users/batch")
                .body(userIds)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public record UserProfileDto(String id, String displayName, String avatarUrl) {}
}
