package com.example.testservice.client;

import com.example.testservice.config.TestServiceFeignConfig;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "attempt-service",
        url = "${internal.services.attempt-url}",
        configuration = TestServiceFeignConfig.class
)
public interface AttemptServiceClient {

    @GetMapping("/attempts-api/internal/attempts/active-exists/{testId}")
    boolean hasActiveAttempts(@PathVariable("testId") UUID testId);
}