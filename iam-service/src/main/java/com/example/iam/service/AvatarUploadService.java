package com.example.iam.service;

import com.cloudinary.Cloudinary;
import com.example.iam.dto.AvatarUploadIntentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvatarUploadService {

    private static final String ASSET_FOLDER = "clearit/profiles";
    private static final String UPLOAD_PRESET = "baahubali_profile_signed";

    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    public AvatarUploadIntentResponse createUploadIntent(UUID userId) {
        long timestamp = System.currentTimeMillis() / 1000;
        String publicId = "profiles/" + userId + "/" + UUID.randomUUID();

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("timestamp", timestamp);
        parameters.put("public_id", publicId);
        parameters.put("asset_folder", ASSET_FOLDER);
        parameters.put("upload_preset", UPLOAD_PRESET);
        parameters.put("overwrite", false);

        String signature = cloudinary.apiSignRequest(parameters, apiSecret, 0);

        Map<String, String> uploadParameters = new LinkedHashMap<>();
        parameters.forEach((key, value) -> uploadParameters.put(key, String.valueOf(value)));

        return new AvatarUploadIntentResponse(
                cloudName,
                apiKey,
                "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload",
                publicId,
                ASSET_FOLDER,
                timestamp,
                signature,
                uploadParameters
        );
    }
}
