package com.example.attemptservice.client;

import com.example.attemptservice.config.TestServiceFeignConfig;
import com.example.attemptservice.dto.internal.InternalTestBlueprintDto;
import com.example.attemptservice.dto.internal.TestServiceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "test-service",
    url = "${internal.services.test-url}",
    configuration = TestServiceFeignConfig.class
)
public interface TestServiceFeignClient {

    @GetMapping("/internal/mock-tests/{id}/blueprint")
    TestServiceResponse<InternalTestBlueprintDto> getTestBlueprint(@PathVariable("id") String testId);

    @org.springframework.web.bind.annotation.PostMapping("/internal/mock-tests/bulk-info")
    TestServiceResponse<java.util.List<com.example.attemptservice.dto.internal.InternalTestBulkInfoDto>> getBulkTestInfo(
            @org.springframework.web.bind.annotation.RequestBody java.util.List<String> testIds
    );
}
