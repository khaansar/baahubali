package com.example.testservice.dto.publiccatalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicCategoryDto {
    private String id;
    private String name;
    private String slug;
    private Long testCount;
}
