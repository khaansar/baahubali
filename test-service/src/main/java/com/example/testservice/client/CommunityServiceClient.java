package com.example.testservice.client;

import com.example.testservice.config.TestServiceFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "community-service",
    url = "${internal.services.community-url}",
    configuration = TestServiceFeignConfig.class
)
public interface CommunityServiceClient {

    @GetMapping("/internal/reviews/{targetId}/average")
    Double getAverageRating(@PathVariable("targetId") String targetId);
}