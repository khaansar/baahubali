package com.example.testservice.dto.publiccatalog;

import java.math.BigDecimal;
import java.util.UUID;

public record PublicMockTestDetailDto(
        UUID id,
        String slug,
        String title,
        String seriesSlug,
        String seriesTitle,
        String categorySlug,
        String categoryName,
        Integer durationMinutes,
        BigDecimal totalMarks,
        boolean isFree
) {
}
