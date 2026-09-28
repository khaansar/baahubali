package com.example.iam.security;

import com.example.iam.config.JwtProperties;
import com.example.iam.entity.User;
import com.example.iam.exception.InvalidRefreshTokenException;
import com.example.iam.exception.RefreshTokenReuseException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Stores only SHA-256 refresh-token hashes in Redis. A token belongs to a family; rotation
 * atomically marks the supplied token as used and replaces it with a new token. Reuse of a
 * previously used token revokes the family and its corresponding gateway session.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TOKEN_PREFIX = "refresh:token:";
    private static final String USED_PREFIX = "refresh:used:";
    private static final String FAMILY_PREFIX = "refresh:family:";
    private static final String USER_FAMILY_PREFIX = "refresh:user-family:";

    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            local family = redis.call('GET', KEYS[1])
            if family then
              redis.call('DEL', KEYS[1])
              redis.call('SET', KEYS[2], family, 'PX', ARGV[2])
              redis.call('SET', KEYS[3], family, 'PX', ARGV[2])
              redis.call('SET', KEYS[4], ARGV[1], 'PX', ARGV[2])
              local separator = string.find(family, '|')
              local familyId = string.sub(family, 1, separator - 1)
              local userAndSession = string.sub(family, separator + 1)
              local userSeparator = string.find(userAndSession, '|')
              local userId = string.sub(userAndSession, 1, userSeparator - 1)
              local userFamilyKey = 'refresh:user-family:' .. userId
              if redis.call('GET', userFamilyKey) == familyId then
                redis.call('PEXPIRE', userFamilyKey, ARGV[2])
                redis.call('PEXPIRE', 'user:session:' .. userId, ARGV[2])
              end
              return 1
            end

            family = redis.call('GET', KEYS[2])
            if family then
              local separator = string.find(family, '|')
              local familyId = string.sub(family, 1, separator - 1)
              local userAndSession = string.sub(family, separator + 1)
              local familyKey = 'refresh:family:' .. familyId
              local current = redis.call('GET', familyKey)
              if current then
                redis.call('DEL', 'refresh:token:' .. current)
              end
              redis.call('DEL', familyKey)
              local userSeparator = string.find(userAndSession, '|')
              local userId = string.sub(userAndSession, 1, userSeparator - 1)
              local userFamilyKey = 'refresh:user-family:' .. userId
              if redis.call('GET', userFamilyKey) == familyId then
                redis.call('DEL', userFamilyKey)
                redis.call('DEL', 'user:session:' .. userId)
              end
              return 2
            end
            return 0
            """, Long.class);

    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    public RefreshTokenService(JwtProperties jwtProperties, StringRedisTemplate redisTemplate) {
        this.jwtProperties = jwtProperties;
        this.redisTemplate = redisTemplate;
    }

    public String issue(User user, String sessionId) {
        revokeForUser(user.getId().toString());

        String familyId = UUID.randomUUID().toString();
        String refreshToken = newToken();
        String tokenHash = hash(refreshToken);
        Duration ttl = Duration.ofMillis(jwtProperties.refreshExpirationMs());
        String familyValue = familyId + "|" + user.getId() + "|" + sessionId;

        redisTemplate.opsForValue().set(TOKEN_PREFIX + tokenHash, familyValue, ttl);
        redisTemplate.opsForValue().set(FAMILY_PREFIX + familyId, tokenHash, ttl);
        redisTemplate.opsForValue().set(USER_FAMILY_PREFIX + user.getId(), familyId, ttl);
        return refreshToken;
    }

    public Rotation rotate(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        String oldHash = hash(refreshToken);
        String familyValue = redisTemplate.opsForValue().get(TOKEN_PREFIX + oldHash);
        if (familyValue == null) {
            rotateOrReject(oldHash, newToken(), null);
            throw new InvalidRefreshTokenException(); // The script either threw or returned an impossible result.
        }

        String[] parts = familyValue.split("\\|", -1);
        if (parts.length != 3) {
            throw new InvalidRefreshTokenException();
        }

        String nextToken = newToken();
        long outcome = rotateOrReject(oldHash, nextToken, parts[0]);
        if (outcome != 1) {
            throw new InvalidRefreshTokenException();
        }
        return new Rotation(UUID.fromString(parts[1]), parts[2], nextToken);
    }

    public void revokeForUser(String userId) {
        String familyId = redisTemplate.opsForValue().get(USER_FAMILY_PREFIX + userId);
        if (familyId == null) {
            return;
        }
        String tokenHash = redisTemplate.opsForValue().get(FAMILY_PREFIX + familyId);
        if (tokenHash != null) {
            redisTemplate.delete(TOKEN_PREFIX + tokenHash);
        }
        redisTemplate.delete(List.of(FAMILY_PREFIX + familyId, USER_FAMILY_PREFIX + userId));
    }

    private long rotateOrReject(String oldHash, String nextToken, String familyId) {
        Long outcome = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(TOKEN_PREFIX + oldHash, USED_PREFIX + oldHash,
                        TOKEN_PREFIX + hash(nextToken), FAMILY_PREFIX + (familyId == null ? "unknown" : familyId)),
                hash(nextToken), Long.toString(jwtProperties.refreshExpirationMs()));
        if (outcome == null || outcome == 0) {
            throw new InvalidRefreshTokenException();
        }
        if (outcome == 2) {
            throw new RefreshTokenReuseException();
        }
        return outcome;
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public record Rotation(UUID userId, String sessionId, String refreshToken) { }
}
