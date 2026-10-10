package com.example.attemptservice.client;

import com.example.attemptservice.config.TestServiceFeignConfig;
import com.example.attemptservice.dto.internal.InternalTestBlueprintDto;
import com.example.attemptservice.dto.internal.InternalTestBulkInfoDto;
import com.example.attemptservice.dto.internal.TestServiceResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "test-service",
        url = "${internal.services.test-url}",
        configuration = TestServiceFeignConfig.class
)
public interface TestServiceFeignClient {

    @GetMapping("/internal/mock-tests/{id}/blueprint")
    TestServiceResponse<InternalTestBlueprintDto> getTestBlueprint(
            @PathVariable("id") String testId);

    @PostMapping("/internal/mock-tests/bulk-info")
    TestServiceResponse<List<InternalTestBulkInfoDto>> getBulkTestInfo(
            @RequestBody List<String> testIds);
}