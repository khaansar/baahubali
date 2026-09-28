package com.example.communityservice.client;

import com.example.communityservice.config.CommunityFeignConfig;
import com.example.communityservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(
    name = "attempt-service",
    url = "${internal.services.attempt-url}",
    configuration = CommunityFeignConfig.class
)
public interface AttemptServiceClient {

    @GetMapping("/internal/attempts/verify")
    ApiResponse<Map<String, Boolean>> hasUserAttemptedTest(
            @RequestParam("userId") String userId,
            @RequestParam("testId") String testId);
}
