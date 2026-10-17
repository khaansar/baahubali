package com.example.communityservice.client;

import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PaymentEntitlementClient {

    private final RestClient http;

    public PaymentEntitlementClient(
            @Value("${internal.services.payment-url}") String url,
            @Value("${internal.auth.my-secret}") String secret,
            RestClient.Builder builder) {
        this.http = builder
                .baseUrl(url)
                .defaultHeader("X-Service-Caller", "community-service")
                .defaultHeader("X-Service-Auth", secret)
                .build();
    }

    public PaymentApiResponse<SeriesEntitlementCheckResponse> checkSeriesEntitlement(
            SeriesEntitlementCheckRequest request) {
        return http.post()
                .uri("/internal/entitlements/check-series")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public record SeriesEntitlementCheckRequest(
            UUID userId,
            UUID seriesId
    ) {}

    public record SeriesEntitlementCheckResponse(
            boolean allowed
    ) {}

    /*
     * Payment service uses a different ApiResponse shape from community-service:
     * { "success": true, "data": { "allowed": true }, ... }
     *
     * Keep this DTO local to avoid assuming both services use identical wrappers.
     */
    public record PaymentApiResponse<T>(
            boolean success,
            T data,
            ErrorBody error,
            Instant timestamp
    ) {
        record ErrorBody(String code, String message) {}
    }
}
