package com.example.communityservice.client;

import com.example.communityservice.config.CommunityFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "attempt-service",
    url = "${internal.services.attempt-url}",
    configuration = CommunityFeignConfig.class
)
public interface AttemptServiceClient {

    @GetMapping("/internal/attempts/verify")
    boolean hasUserAttemptedTest(@RequestParam("userId") String userId, @RequestParam("testId") String testId);
}