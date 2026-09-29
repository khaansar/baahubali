package com.example.testservice.controller;

import com.example.testservice.dto.ApiResponse;
import com.example.testservice.dto.internal.TestBlueprintDto;
import com.example.testservice.dto.internal.TestStatusDto;
import com.example.testservice.service.InternalMockTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/mock-tests")
@RequiredArgsConstructor
public class InternalMockTestController {

    private final InternalMockTestService internalService;

    @GetMapping("/{id}/blueprint")
    public ResponseEntity<ApiResponse<TestBlueprintDto>> getTestBlueprint(@PathVariable UUID id) {
        TestBlueprintDto blueprint = internalService.getPublishedBlueprint(id);
        return ResponseEntity.ok(ApiResponse.success(blueprint));
    }
    
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TestStatusDto>> getTestStatus(@PathVariable UUID id) {
        TestStatusDto status = internalService.getLightweightStatus(id);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @PostMapping("/bulk-info")
    public ResponseEntity<ApiResponse<java.util.List<com.example.testservice.dto.internal.TestBulkInfoDto>>> getBulkTestInfo(@RequestBody java.util.List<UUID> testIds) {
        java.util.List<com.example.testservice.dto.internal.TestBulkInfoDto> info = internalService.getBulkTestInfo(testIds);
        return ResponseEntity.ok(ApiResponse.success(info));
    }
}