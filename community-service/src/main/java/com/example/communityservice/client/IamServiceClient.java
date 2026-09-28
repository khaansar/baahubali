package com.example.communityservice.client;

import com.example.communityservice.config.CommunityFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(
    name = "iam-service",
    url = "${internal.services.iam-url}",
    configuration = CommunityFeignConfig.class
)
public interface IamServiceClient {

    @PostMapping("/internal/users/batch")
    Map<String, UserProfileDto> getUsersBatch(@RequestBody List<String> userIds);

    record UserProfileDto(String userId, String name, String avatarUrl) {}
}