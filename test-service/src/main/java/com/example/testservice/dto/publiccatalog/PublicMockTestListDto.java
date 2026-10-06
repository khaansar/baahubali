package com.example.testservice.dto.publiccatalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicMockTestListDto {
    private UUID id;
    private String slug;
    private String title;
    private String type; // This will map to series title
    private String categoryName;
    private String categorySlug;
    private Integer durationMinutes;
    private BigDecimal totalMarks;
    private boolean isFree;
}
