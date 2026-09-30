package com.example.testservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private String id;
    private String name;
    private String slug;
    private String description;
    private java.util.List<String> requiredLanguages;
    private java.time.Instant createdAt;
    private java.time.Instant updatedAt;
    private java.time.Instant deletedAt;
}