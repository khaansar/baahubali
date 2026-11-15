package com.example.iam.security;

import com.example.iam.config.JwtProperties;
import com.example.iam.entity.User;
import com.example.iam.exception.InvalidRefreshTokenException;
import com.example.iam.exception.RefreshTokenReuseException;
import com.example.iam.exception.RefreshTokenSupersededException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
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
 *
 * <p>Exception: for a short grace period after rotation, presenting the just-rotated token again
 * returns the same replacement token. This covers two browser tabs refreshing at once and a
 * response lost on the network, which would otherwise look like theft and log the user out. The
 * replacement is stored encrypted with a key derived from the old token, so Redis still holds no
 * usable token.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TOKEN_PREFIX = "refresh:token:";
    private static final String USED_PREFIX = "refresh:used:";
    private static final String GRACE_PREFIX = "refresh:grace:";
    private static final String FAMILY_PREFIX = "refresh:family:";
    private static final String USER_FAMILY_PREFIX = "refresh:user-family:";

    private static final String ROTATED = "ROTATED";
    private static final String GRACE = "GRACE|";
    private static final String SUPERSEDED = "SUPERSEDED";
    private static final String REUSED = "REUSED";

    private static final String SEAL_CIPHER = "AES/GCM/NoPadding";
    private static final int SEAL_IV_BYTES = 12;
    private static final int SEAL_TAG_BITS = 128;

    /*
     * KEYS: 1 current-token key of the presented token, 2 its used-token key, 3 current-token key
     *       of the replacement, 4 family key, 5 grace key of the presented token.
     * ARGV: 1 replacement hash, 2 refresh TTL ms, 3 sealed replacement, 4 grace ms.
     */
    private static final DefaultRedisScript<String> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            local family = redis.call('GET', KEYS[1])
            if family then
              redis.call('DEL', KEYS[1])
              redis.call('SET', KEYS[2], family, 'PX', ARGV[2])
              redis.call('SET', KEYS[3], family, 'PX', ARGV[2])
              redis.call('SET', KEYS[4], ARGV[1], 'PX', ARGV[2])
              if tonumber(ARGV[4]) > 0 then
                redis.call('SET', KEYS[5], ARGV[1] .. '|' .. ARGV[3], 'PX', ARGV[4])
              end
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
              return 'ROTATED'
            end

            family = redis.call('GET', KEYS[2])
            if family then
              local separator = string.find(family, '|')
              local familyId = string.sub(family, 1, separator - 1)
              local userAndSession = string.sub(family, separator + 1)
              local familyKey = 'refresh:family:' .. familyId
              local current = redis.call('GET', familyKey)

              local grace = redis.call('GET', KEYS[5])
              if grace then
                local graceSeparator = string.find(grace, '|')
                local replacementHash = string.sub(grace, 1, graceSeparator - 1)
                if current == replacementHash then
                  return 'GRACE|' .. string.sub(grace, graceSeparator + 1)
                end
                if current then
                  return 'SUPERSEDED'
                end
              end

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
              return 'REUSED'
            end
            return 'INVALID'
            """, String.class);

    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    public RefreshTokenService(JwtProperties jwtProperties, StringRedisTemplate redisTemplate) {
        if (jwtProperties.refreshReuseGraceMs() < 0
                || jwtProperties.refreshReuseGraceMs() >= jwtProperties.refreshExpirationMs()) {
            throw new IllegalStateException(
                    "app.jwt.refresh-reuse-grace-ms (REFRESH_TOKEN_REUSE_GRACE_MS) must be at least 0 and shorter "
                            + "than the refresh token lifetime, but was " + jwtProperties.refreshReuseGraceMs());
        }
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
        // A token that was already rotated is found through its used-token record, which keeps
        // the family needed for the grace check and for reuse detection.
        String familyValue = redisTemplate.opsForValue().get(TOKEN_PREFIX + oldHash);
        if (familyValue == null) {
            familyValue = redisTemplate.opsForValue().get(USED_PREFIX + oldHash);
        }
        if (familyValue == null) {
            throw new InvalidRefreshTokenException();
        }

        String[] parts = familyValue.split("\\|", -1);
        if (parts.length != 3) {
            throw new InvalidRefreshTokenException();
        }

        String nextToken = newToken();
        String nextHash = hash(nextToken);
        String outcome = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(TOKEN_PREFIX + oldHash, USED_PREFIX + oldHash, TOKEN_PREFIX + nextHash,
                        FAMILY_PREFIX + parts[0], GRACE_PREFIX + oldHash),
                nextHash,
                Long.toString(jwtProperties.refreshExpirationMs()),
                seal(refreshToken, nextToken),
                Long.toString(jwtProperties.refreshReuseGraceMs()));

        if (ROTATED.equals(outcome)) {
            return new Rotation(UUID.fromString(parts[1]), parts[2], nextToken);
        }
        if (outcome != null && outcome.startsWith(GRACE)) {
            String replacement = unseal(refreshToken, outcome.substring(GRACE.length()));
            return new Rotation(UUID.fromString(parts[1]), parts[2], replacement);
        }
        if (SUPERSEDED.equals(outcome)) {
            throw new RefreshTokenSupersededException();
        }
        if (REUSED.equals(outcome)) {
            throw new RefreshTokenReuseException();
        }
        throw new InvalidRefreshTokenException();
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

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Encrypts the replacement token with a key only the holder of the old token can derive. */
    private static String seal(String oldToken, String replacement) {
        try {
            byte[] iv = new byte[SEAL_IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(SEAL_CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, sealKey(oldToken), new GCMParameterSpec(SEAL_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(replacement.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not seal the replacement refresh token", ex);
        }
    }

    private static String unseal(String oldToken, String sealed) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(sealed);
            Cipher cipher = Cipher.getInstance(SEAL_CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, sealKey(oldToken),
                    new GCMParameterSpec(SEAL_TAG_BITS, bytes, 0, SEAL_IV_BYTES));
            byte[] plain = cipher.doFinal(bytes, SEAL_IV_BYTES, bytes.length - SEAL_IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new InvalidRefreshTokenException();
        }
    }

    private static SecretKeySpec sealKey(String oldToken) throws GeneralSecurityException {
        byte[] key = MessageDigest.getInstance("SHA-256")
                .digest(("refresh-grace-v1:" + oldToken).getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
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
