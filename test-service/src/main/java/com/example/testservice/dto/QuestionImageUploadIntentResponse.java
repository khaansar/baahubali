package com.example.testservice.dto;

import java.util.Map;

public record QuestionImageUploadIntentResponse(
        String cloudName,
        String apiKey,
        String uploadUrl,
        String publicId,
        String assetFolder,
        long timestamp,
        String signature,
        Map<String, String> uploadParameters
) {
}