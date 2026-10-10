package com.example.communityservice.dto.response;

import java.util.List;

public record AdminReviewPageDto(
        List<AdminReviewResponseDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {}
