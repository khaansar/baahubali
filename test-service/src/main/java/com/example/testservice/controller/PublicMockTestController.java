package com.example.testservice.controller;

import com.example.testservice.dto.ApiResponse;
import com.example.testservice.dto.common.PaginatedResponseDto;
import com.example.testservice.dto.publiccatalog.PublicMockTestListDto;
import com.example.testservice.service.publiccatalog.impl.PublicMockTestServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/public/mock-tests")
@RequiredArgsConstructor
public class PublicMockTestController {

    private final PublicMockTestServiceImpl publicMockTestService;

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<PublicMockTestListDto>>> getPublishedMockTests(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        PaginatedResponseDto<PublicMockTestListDto> response =
                publicMockTestService.getPublishedMockTests(categoryId, query, page, limit);

        return ResponseEntity.ok(ApiResponse.paginated(response.data(), response.meta()));
    }
}
