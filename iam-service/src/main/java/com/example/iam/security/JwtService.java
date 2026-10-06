package com.example.iam.security;

import com.example.iam.config.JwtProperties;
import com.example.iam.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.io.Decoders;
import javax.crypto.SecretKey;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * Issues JWTs on successful registration/login. Token verification is intentionally NOT
 * performed here — per the platform architecture, the API Gateway is the single point of
 * JWT validation for the whole system. This service only ever needs to mint tokens using
 * the shared JWT_SECRET; the Gateway verifies them using that same secret.
 */
@Service
public class JwtService {

    private static final String CLAIM_ROLE = "role";

    private final JwtProperties jwtProperties;
    private final Key signingKey;
    private final StringRedisTemplate redisTemplate;

    public JwtService(JwtProperties jwtProperties, StringRedisTemplate redisTemplate) {
        this.jwtProperties = jwtProperties;
        this.redisTemplate = redisTemplate;
        this.signingKey = resolveSigningKey(jwtProperties);
    }

    public String createSession(User user) {
        String sessionId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                "user:session:" + user.getId(), sessionId, Duration.ofMillis(jwtProperties.refreshExpirationMs()));
        return sessionId;
    }

    public String generateToken(User user, String sessionId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.expirationMs());

        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(jwtProperties.issuer())
                .audience().add(jwtProperties.audience()).and()
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim("sessionId", sessionId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    private static Key resolveSigningKey(JwtProperties properties) {
        if ("RS256".equalsIgnoreCase(properties.signingAlgorithm())) {
            if (properties.privateKey() == null || properties.privateKey().isBlank()) {
                throw new IllegalStateException("app.jwt.private-key is required for RS256");
            }

            try {
                String pem = java.nio.file.Files.readString(
                        java.nio.file.Path.of(properties.privateKey())
                );

                String privateKeyContent = pem
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");

                PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(
                        new PKCS8EncodedKeySpec(
                                Decoders.BASE64.decode(privateKeyContent)
                        )
                );

                return key;
            } catch (Exception ex) {
                throw new IllegalStateException(
                        "app.jwt.private-key is not a valid PKCS#8 RSA private key file",
                        ex
                );
            }
        }

        if ("HS256".equalsIgnoreCase(properties.signingAlgorithm())
                && properties.secret() != null) {
            return Keys.hmacShaKeyFor(
                    Decoders.BASE64.decode(properties.secret())
            );
        }

        throw new IllegalStateException(
                "Only RS256 is permitted outside explicit HS256 test compatibility mode"
        );
    }
}
