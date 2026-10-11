package com.example.iam.service;

import com.example.iam.entity.User;
import com.example.iam.exception.InvalidCredentialsException;
import com.example.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvatarProfileService {

    private final UserRepository userRepository;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Transactional
    public User updateAvatar(UUID userId, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank() || avatarUrl.length() > 512) {
            throw new IllegalArgumentException("Invalid avatar URL");
        }

        URI uri;
        try {
            uri = URI.create(avatarUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid avatar URL");
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"res.cloudinary.com".equalsIgnoreCase(uri.getHost())
                || uri.getUserInfo() != null
                || uri.getPort() != -1
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("Avatar must be a valid Cloudinary HTTPS URL");
        }

        String path = uri.getPath();
        String expectedPrefix = "/" + cloudName + "/image/upload/";

        if (path == null || !path.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException("Avatar URL does not belong to the configured Cloudinary account");
        }

        String uploadedAssetPath = path.substring(expectedPrefix.length());
        String[] segments = uploadedAssetPath.split("/");

        int publicIdStart = segments.length > 0 && segments[0].matches("v\\d+")
                ? 1
                : 0;

        if (segments.length < publicIdStart + 3
                || !"profiles".equals(segments[publicIdStart])
                || !userId.toString().equals(segments[publicIdStart + 1])) {
            throw new IllegalArgumentException("Avatar must belong to the authenticated user's profile folder");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        user.setAvatarUrl(avatarUrl);
        return userRepository.save(user);
    }
}