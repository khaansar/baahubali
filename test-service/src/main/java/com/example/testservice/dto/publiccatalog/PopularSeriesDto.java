package com.example.testservice.dto.publiccatalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PopularSeriesDto {
    private String id;
    private String slug;
    private String title;
    private String badge;
    private Long testCount;
    private Integer durationMinutes;
    private String thumbnailUrl;
}