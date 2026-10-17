package com.example.attemptservice.client;

import com.example.attemptservice.dto.internal.InternalTestBlueprintDto;
import com.example.attemptservice.dto.internal.InternalTestBulkInfoDto;
import com.example.attemptservice.dto.internal.TestServiceResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
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
                .defaultHeader("X-Service-Caller", "attempt-service")
                .defaultHeader("X-Service-Auth", secret)
                .build();
    }

    public TestServiceResponse<InternalTestBlueprintDto> getTestBlueprint(String testId) {
        return http.get()
                .uri("/internal/mock-tests/{id}/blueprint", testId)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<>() {});
    }

    public TestServiceResponse<List<InternalTestBulkInfoDto>> getBulkTestInfo(List<String> testIds) {
        return http.post()
                .uri("/internal/mock-tests/bulk-info")
                .body(testIds)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<>() {});
    }
}
