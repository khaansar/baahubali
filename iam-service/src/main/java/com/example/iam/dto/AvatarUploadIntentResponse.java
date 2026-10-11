package com.example.iam.dto;

import java.util.Map;

public record AvatarUploadIntentResponse(
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
