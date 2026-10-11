package com.example.iam.service;

import com.cloudinary.Cloudinary;
import com.example.iam.entity.User;
import com.example.iam.exception.InvalidCredentialsException;
import com.example.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvatarProfileService {

    private static final String ASSET_FOLDER = "clearit/profiles";
    private static final String PUBLIC_ID_PREFIX = "profiles/";

    private final UserRepository userRepository;
    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Transactional
    public User updateAvatar(UUID userId, String avatarUrl) {
        if (userId == null
                || avatarUrl == null
                || avatarUrl.isBlank()
                || avatarUrl.length() > 512) {
            throw new IllegalArgumentException("Invalid avatar URL");
        }

        String normalizedUrl = avatarUrl.trim();

        URI uri;
        try {
            uri = URI.create(normalizedUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid avatar URL");
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"res.cloudinary.com".equalsIgnoreCase(uri.getHost())
                || uri.getUserInfo() != null
                || uri.getPort() != -1
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw new IllegalArgumentException(
                    "Avatar must be a valid Cloudinary HTTPS URL");
        }

        String path = uri.getPath();
        String expectedPrefix = "/" + cloudName + "/image/upload/";

        if (path == null || !path.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException(
                    "Avatar URL does not belong to the configured Cloudinary account");
        }

        String uploadedAssetPath = path.substring(expectedPrefix.length());
        String[] segments = uploadedAssetPath.split("/");

        int publicIdStart = segments.length > 0
                && segments[0].matches("v\\d+")
                ? 1
                : 0;

        /*
         * Expected URL path:
         * /<cloud-name>/image/upload/v<version>/profiles/<user-id>/<asset-id>.<format>
         *
         * The version segment is optional because Cloudinary URLs can omit it.
         */
        if (segments.length != publicIdStart + 3
                || !"profiles".equals(segments[publicIdStart])
                || !userId.toString().equals(segments[publicIdStart + 1])) {
            throw new IllegalArgumentException(
                    "Avatar must belong to the authenticated user's profile folder");
        }

        String filename = segments[publicIdStart + 2];
        int extensionSeparator = filename.lastIndexOf('.');

        if (extensionSeparator <= 0 || extensionSeparator == filename.length() - 1) {
            throw new IllegalArgumentException(
                    "Avatar URL must contain a valid image format");
        }

        String publicId = PUBLIC_ID_PREFIX
                + userId
                + "/"
                + filename.substring(0, extensionSeparator);

        if (publicId.contains("..")
                || publicId.contains("\\")
                || publicId.contains("?")
                || publicId.contains("#")) {
            throw new IllegalArgumentException("Invalid avatar public ID");
        }

        verifyCloudinaryAsset(publicId, normalizedUrl);

        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        user.setAvatarUrl(normalizedUrl);
        return userRepository.save(user);
    }

    private void verifyCloudinaryAsset(String publicId, String avatarUrl) {
        try {
            Map<?, ?> asset = cloudinary.api().resource(
                    publicId,
                    new HashMap<>());

            String actualPublicId = String.valueOf(asset.get("public_id"));
            String actualResourceType = String.valueOf(asset.get("resource_type"));
            String actualSecureUrl = String.valueOf(asset.get("secure_url"));
            String actualAssetFolder = String.valueOf(asset.get("asset_folder"));
            String actualFormat = String.valueOf(asset.get("format"));

            if (!publicId.equals(actualPublicId)
                    || !"image".equalsIgnoreCase(actualResourceType)
                    || actualFormat.isBlank()
                    || "null".equals(actualFormat)
                    || !ASSET_FOLDER.equals(actualAssetFolder)
                    || !avatarUrl.equals(actualSecureUrl)) {
                throw new IllegalArgumentException(
                        "Avatar is not a verified image belonging to this profile");
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Avatar image could not be verified with Cloudinary");
        }
    }
}