package com.example.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.example.apigateway.config.GatewaySecurityProperties;
import com.example.apigateway.exception.ExpiredTokenException;
import com.example.apigateway.exception.InvalidTokenException;
import reactor.core.publisher.Mono;

@Component
public class JwtTokenValidator implements TokenValidator {

    private final JwtParser jwtParser;
    private final String userIdClaim;
    private final String roleClaim;
    private final ReactiveStringRedisTemplate redisTemplate;

    public JwtTokenValidator(GatewaySecurityProperties properties, ReactiveStringRedisTemplate redisTemplate) {
        GatewaySecurityProperties.Jwt jwt = properties.jwt();
        this.userIdClaim = jwt.userIdClaim();
        this.roleClaim = jwt.roleClaim();
        this.redisTemplate = redisTemplate;
        this.jwtParser = buildParser(jwt);
    }

    @Override
    public Mono<AuthenticatedUser> validate(String token) {
        return Mono.fromCallable(() -> {
            try {
                return jwtParser.parseSignedClaims(token).getPayload();
            } catch (ExpiredJwtException ex) {
                throw new ExpiredTokenException(ex);
            } catch (JwtException | IllegalArgumentException ex) {
                throw new InvalidTokenException("JWT could not be verified: " + ex.getMessage(), ex);
            }
        }).flatMap(claims -> {
            String userId = readScalarClaim(claims, userIdClaim);
            String role = readScalarClaim(claims, roleClaim);
            String sessionId = readScalarClaim(claims, "sessionId");

            if (!StringUtils.hasText(userId) || !StringUtils.hasText(role) || !StringUtils.hasText(sessionId)) {
                return Mono.error(new InvalidTokenException(
                        "JWT is missing required claims [" + userIdClaim + ", " + roleClaim + ", sessionId]"));
            }

            // Enforce Single Login: Verify the session in the JWT matches the active session in Redis
            return redisTemplate.opsForValue().get("user:session:" + userId)
                    .switchIfEmpty(Mono.error(new InvalidTokenException("Session expired or user logged out.")))
                    .flatMap(activeSessionId -> {
                        if (!activeSessionId.equals(sessionId)) {
                            return Mono.error(new InvalidTokenException("Logged in from another device. Session invalidated."));
                        }
                        return Mono.just(new AuthenticatedUser(userId, role));
                    });
        });
    }

    private static String readScalarClaim(Claims claims, String name) {
        Object value = claims.get(name);
        return (value instanceof String || value instanceof Number) ? value.toString() : null;
    }

    private static JwtParser buildParser(GatewaySecurityProperties.Jwt jwt) {
        JwtParserBuilder builder = Jwts.parser()
                .clockSkewSeconds(jwt.clockSkewSeconds());
        if ("RS256".equalsIgnoreCase(jwt.algorithm())) {
            builder.verifyWith(readRsaPublicKey(jwt.publicKey()));
        } else if ("HS256".equalsIgnoreCase(jwt.algorithm())) {
            if (!StringUtils.hasText(jwt.secret())) {
                throw new IllegalStateException("gateway.security.jwt.secret is required for HS256");
            }
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.secret()));
            builder.verifyWith(key);
        } else {
            throw new IllegalStateException("Unsupported JWT algorithm: " + jwt.algorithm());
        }
        builder.requireIssuer(jwt.issuer());
        builder.requireAudience(jwt.audience());
        return builder.build();
    }

    private static PublicKey readRsaPublicKey(String keyPath) {
        if (!StringUtils.hasText(keyPath)) {
            throw new IllegalStateException("gateway.security.jwt.public-key-path is required for RS256");
        }
        try {
            String pem = Files.readString(Path.of(keyPath));

            String publicKeyContent = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            return KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(Decoders.BASE64.decode(publicKeyContent)));
        } catch (Exception ex) {
            throw new IllegalStateException("gateway.security.jwt.public-key-path is not a valid X.509 RSA public key", ex);
        }
    }
}