package com.example.iam.service;

import com.example.iam.config.OtpProperties;
import com.example.iam.exception.OtpCooldownException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Issues and checks 6-digit one-time codes. Only a hash of each code is stored, in Redis,
 * together with a wrong-attempt counter; a code is single-use.
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    public enum Purpose {
        EMAIL_VERIFICATION("email-verification"),
        ACCOUNT_DELETION("account-deletion");

        private final String key;

        Purpose(String key) {
            this.key = key;
        }
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Returns 1 when the code matches (and consumes it), 0 when it does not, and discards the code
     * once the allowed wrong attempts are used up.
     */
    private static final DefaultRedisScript<Long> VERIFY_SCRIPT = new DefaultRedisScript<>("""
            local stored = redis.call('HGET', KEYS[1], 'hash')
            if not stored then
              return 0
            end
            if stored == ARGV[1] then
              redis.call('DEL', KEYS[1])
              return 1
            end
            local attempts = redis.call('HINCRBY', KEYS[1], 'attempts', 1)
            if attempts >= tonumber(ARGV[2]) then
              redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final OtpProperties properties;

    /**
     * Creates a new code for the user, replacing any previous one.
     *
     * @throws OtpCooldownException if a code was issued less than the resend cooldown ago
     */
    public String issue(Purpose purpose, UUID userId) {
        String cooldownKey = cooldownKey(purpose, userId);

        if (!properties.resendCooldown().isZero()) {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(cooldownKey, "1", properties.resendCooldown());

            if (!Boolean.TRUE.equals(acquired)) {
                Long remaining = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
                throw new OtpCooldownException(remaining == null || remaining < 1 ? 1 : remaining);
            }
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        String codeKey = codeKey(purpose, userId);

        redisTemplate.delete(codeKey);
        redisTemplate.opsForHash().putAll(codeKey, Map.of(
                "hash", hash(purpose, userId, code),
                "attempts", "0"
        ));
        redisTemplate.expire(codeKey, properties.ttl());

        return code;
    }

    /** Checks the code and consumes it on success. Wrong codes count towards the attempt limit. */
    public boolean verify(Purpose purpose, UUID userId, String code) {
        if (code == null || !code.matches("[0-9]{6}")) {
            return false;
        }

        Long result = redisTemplate.execute(
                VERIFY_SCRIPT,
                List.of(codeKey(purpose, userId)),
                hash(purpose, userId, code),
                String.valueOf(properties.maxAttempts())
        );

        return Long.valueOf(1L).equals(result);
    }

    public Duration ttl() {
        return properties.ttl();
    }

    private static String codeKey(Purpose purpose, UUID userId) {
        return "otp:" + purpose.key + ":" + userId;
    }

    private static String cooldownKey(Purpose purpose, UUID userId) {
        return "otp:" + purpose.key + ":" + userId + ":cooldown";
    }

    private static String hash(Purpose purpose, UUID userId, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = digest.digest(
                    (purpose.key + ":" + userId + ":" + code).getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(value);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
