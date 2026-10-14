package com.example.communityservice.client;

import com.example.communityservice.config.CommunityFeignConfig;
import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;
import java.util.UUID;

@FeignClient(
        name = "payment-service",
        url = "${internal.services.payment-url}",
        configuration = CommunityFeignConfig.class
)
public interface PaymentEntitlementClient {

    @PostMapping("/internal/entitlements/check-series")
    PaymentApiResponse<SeriesEntitlementCheckResponse> checkSeriesEntitlement(
            @RequestBody SeriesEntitlementCheckRequest request
    );

    record SeriesEntitlementCheckRequest(
            UUID userId,
            UUID seriesId
    ) {}

    record SeriesEntitlementCheckResponse(
            boolean allowed
    ) {}

    /*
     * Payment service uses a different ApiResponse shape from community-service:
     * { "success": true, "data": { "allowed": true }, ... }
     *
     * Keep this DTO local to avoid assuming both services use identical wrappers.
     */
    record PaymentApiResponse<T>(
            boolean success,
            T data,
            ErrorBody error,
            Instant timestamp
    ) {
        record ErrorBody(String code, String message) {}
    }
}