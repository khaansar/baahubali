package com.example.communityservice.client;

import com.example.communityservice.dto.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TestSeriesClient {

    private final RestClient http;

    public TestSeriesClient(
            @Value("${internal.services.test-url}") String url,
            @Value("${internal.auth.my-secret}") String secret,
            RestClient.Builder builder) {
        this.http = builder
                .baseUrl(url)
                .defaultHeader("X-Service-Caller", "community-service")
                .defaultHeader("X-Service-Auth", secret)
                .build();
    }

    public ApiResponse<List<SeriesNameDto>> getSeriesNames(List<UUID> seriesIds) {
        return http.post()
                .uri("/internal/series/names")
                .body(seriesIds)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public ApiResponse<SeriesReviewAccessDto> getSeriesReviewAccess(UUID seriesId) {
        return http.get()
                .uri("/internal/series/{seriesId}/review-access", seriesId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public record SeriesNameDto(
            UUID seriesId,
            String name
    ) {}

    public record SeriesReviewAccessDto(
            UUID seriesId,
            boolean paid
    ) {}
}
