package com.example.communityservice.client;

import com.example.communityservice.config.CommunityFeignConfig;
import com.example.communityservice.dto.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "iam-service",
        url = "${internal.services.iam-url}",
        configuration = CommunityFeignConfig.class
)
public interface IamServiceClient {

    @PostMapping("/auth-api/internal/users/batch")
    ApiResponse<Map<String, UserProfileDto>> getUsersBatch(
            @RequestBody List<String> userIds);

    record UserProfileDto(String id, String displayName, String avatarUrl) {}
}