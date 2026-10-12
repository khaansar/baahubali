package com.example.analyticsservice.client;

import com.example.analyticsservice.config.AnalyticsFeignConfig;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "test-service",
        url = "${internal.services.test-url}",
        configuration = AnalyticsFeignConfig.class
)
public interface TestServiceClient {

    @GetMapping("/internal/mock-tests/slug/{slug}/leaderboard-status")
    ServiceResponse<PublishedTestStatus> getPublishedTestStatusBySlug(
            @PathVariable("slug") String slug);

    record PublishedTestStatus(UUID testId) {
    }
}
