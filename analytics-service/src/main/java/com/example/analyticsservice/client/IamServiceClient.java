package com.example.analyticsservice.client;

import com.example.analyticsservice.config.AnalyticsFeignConfig;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "iam-service",
        url = "${internal.services.iam-url}",
        configuration = AnalyticsFeignConfig.class
)
public interface IamServiceClient {

    @PostMapping("/internal/users/batch")
    ServiceResponse<Map<String, UserProfile>> getUsersBatch(@RequestBody List<String> userIds);

    record UserProfile(String id, String displayName, String avatarUrl) {
    }
}
