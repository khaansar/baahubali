package com.example.testservice.dto.admin;

import java.util.List;
import java.util.UUID;

public record BulkImportResultDto(
        int imported,
        int failed,
        List<ImportError> errors,
        List<UUID> createdIds
) {
    public record ImportError(int row, String reason) {}
}