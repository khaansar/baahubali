package com.example.iam.service;

import com.example.iam.entity.User;
import com.example.iam.event.IamEventPublisher;
import com.example.iam.exception.InvalidEmailVerificationTokenException;
import com.example.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String TOKEN_KEY_PREFIX = "email-verification:token:";
    private static final String USER_KEY_PREFIX = "email-verification:user:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final IamEventPublisher iamEventPublisher;

    @Value("${app.email-verification.base-url}")
    private String verificationBaseUrl;

    public void issueVerificationEmail(User user) {
        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);

        String userKey = USER_KEY_PREFIX + user.getId();
        String tokenKey = TOKEN_KEY_PREFIX + tokenHash;

        // Invalidate an older verification token.
        String previousTokenHash = redisTemplate.opsForValue().get(userKey);
        if (previousTokenHash != null) {
            redisTemplate.delete(TOKEN_KEY_PREFIX + previousTokenHash);
        }

        // token hash -> userId
        redisTemplate.opsForValue().set(tokenKey, user.getId().toString(), TOKEN_TTL);

        // userId -> current token hash
        redisTemplate.opsForValue().set(userKey, tokenHash, TOKEN_TTL);

        String verificationUrl = verificationBaseUrl + "?token=" + rawToken;

        iamEventPublisher.publishEmailVerificationRequested(user, verificationUrl);
    }

    public boolean isVerificationTokenValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }

        String tokenHash = hashToken(rawToken);

        return Boolean.TRUE.equals(
                redisTemplate.hasKey(TOKEN_KEY_PREFIX + tokenHash)
        );
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidEmailVerificationTokenException();
        }

        String tokenHash = hashToken(rawToken);
        String tokenKey = TOKEN_KEY_PREFIX + tokenHash;
        String userId = redisTemplate.opsForValue().get(tokenKey);

        if (userId == null) {
            throw new InvalidEmailVerificationTokenException();
        }

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(InvalidEmailVerificationTokenException::new);

        // Idempotent DB state.
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        // Verification tokens are single-use.
        deleteVerificationKeys(user.getId(), tokenHash);
    }

    public void resendVerificationEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase();

        userRepository.findByEmail(normalizedEmail)
                .filter(user -> !Boolean.TRUE.equals(user.getEmailVerified()))
                .ifPresent(this::issueVerificationEmail);
    }

    private void deleteVerificationKeys(UUID userId, String tokenHash) {
        redisTemplate.delete(TOKEN_KEY_PREFIX + tokenHash);
        redisTemplate.delete(USER_KEY_PREFIX + userId);
    }

    private String generateToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder(hash.length * 2);

            for (byte value : hash) {
                result.append(String.format("%02x", value));
            }

            return result.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    e
            );
        }
    }
}