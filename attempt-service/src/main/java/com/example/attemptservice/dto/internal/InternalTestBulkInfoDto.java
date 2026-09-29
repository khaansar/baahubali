package com.example.attemptservice.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalTestBulkInfoDto {
    private String testId;
    private String testName;
    private String categoryName;
}
