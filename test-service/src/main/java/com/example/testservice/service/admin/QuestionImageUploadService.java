package com.example.testservice.service.admin;

import com.cloudinary.Cloudinary;
import com.example.common.audit.AuditEvent;
import com.example.testservice.dto.QuestionImageUploadIntentResponse;
import com.example.testservice.service.KafkaPublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class QuestionImageUploadService {

    private static final String ASSET_FOLDER = "clearit/questions";
    private static final String PUBLIC_ID_PREFIX = "questions/";

    private final Cloudinary cloudinary;
    private final KafkaPublisherService kafkaPublisherService;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    public QuestionImageUploadIntentResponse createUploadIntent(String adminId) {
        if (adminId == null || adminId.isBlank()) {
            throw new IllegalArgumentException("Admin ID is required to create a question image upload intent");
        }

        long timestamp = System.currentTimeMillis() / 1000;
        String publicId = PUBLIC_ID_PREFIX + UUID.randomUUID();

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("timestamp", timestamp);
        parameters.put("public_id", publicId);
        parameters.put("asset_folder", ASSET_FOLDER);
        parameters.put("overwrite", false);

        String signature = cloudinary.apiSignRequest(parameters, apiSecret, 0);

        Map<String, String> uploadParameters = new LinkedHashMap<>();
        parameters.forEach((key, value) -> uploadParameters.put(key, String.valueOf(value)));

        kafkaPublisherService.emitAuditEvent(new AuditEvent(
                UUID.randomUUID(),
                adminId,
                "ADMIN",
                "QUESTION_IMAGE_UPLOAD_INTENT_CREATED",
                "QUESTION_IMAGE",
                publicId,
                "test-service",
                "/admin/question-images/upload-intent",
                "POST",
                200,
                null,
                Map.of("assetFolder", ASSET_FOLDER),
                Instant.now()
        ));

        return new QuestionImageUploadIntentResponse(
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
