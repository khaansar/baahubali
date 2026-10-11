package com.example.testservice.service.admin.impl;

import com.cloudinary.Cloudinary;
import java.util.HashMap;
import com.example.testservice.dto.QuestionImageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class QuestionImageMetadataValidator {

    private static final String ASSET_FOLDER = "clearit/questions";
    private static final String PUBLIC_ID_PREFIX = "questions/";

    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    public void validateImages(Iterable<QuestionImageDto> images) {
        if (images == null) {
            return;
        }

        for (QuestionImageDto image : images) {
            validateImage(image);
        }
    }

    public void validateImage(QuestionImageDto image) {
        if (image == null || image.getUrl() == null || image.getPublicId() == null) {
            throw invalidImage();
        }

        String publicId = image.getPublicId().trim();
        if (!publicId.startsWith(PUBLIC_ID_PREFIX) || publicId.contains("..") || publicId.contains("\\")) {
            throw invalidImage();
        }

        URI uri;
        try {
            uri = URI.create(image.getUrl().trim());
        } catch (IllegalArgumentException exception) {
            throw invalidImage();
        }

        String expectedHost = "res.cloudinary.com";
        String expectedPathPrefix = "/" + cloudName + "/image/upload/";

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !expectedHost.equalsIgnoreCase(uri.getHost())
                || uri.getUserInfo() != null
                || uri.getPort() != -1
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || uri.getPath() == null
                || !uri.getPath().startsWith(expectedPathPrefix)) {
            throw invalidImage();
        }

        try {
            Map<?, ?> asset = cloudinary.api().resource(publicId, new HashMap<>());

            String actualPublicId = String.valueOf(asset.get("public_id"));
            String actualResourceType = String.valueOf(asset.get("resource_type"));
            String actualFormat = String.valueOf(asset.get("format"));
            String actualSecureUrl = String.valueOf(asset.get("secure_url"));
            String actualAssetFolder = String.valueOf(asset.get("asset_folder"));

            if (!publicId.equals(actualPublicId)
                    || !"image".equalsIgnoreCase(actualResourceType)
                    || actualFormat.isBlank()
                    || !"null".equals(actualAssetFolder) && !ASSET_FOLDER.equals(actualAssetFolder)
                    || !image.getUrl().trim().equals(actualSecureUrl)) {
                throw invalidImage();
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question image could not be verified");
        }
    }

    private ResponseStatusException invalidImage() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or unverified question image metadata");
    }
}