package com.example.testservice.dto.publiccatalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeaturedMockTestDto {
    private String id;
    private String title;
    private String categoryName;
    private Integer durationMinutes;
    private BigDecimal totalMarks;
}
