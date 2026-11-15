package com.example.iam.security;

import com.example.iam.config.JwtProperties;
import com.example.iam.entity.User;
import com.example.iam.exception.InvalidRefreshTokenException;
import com.example.iam.exception.RefreshTokenReuseException;
import com.example.iam.exception.RefreshTokenSupersededException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Exercises refresh-token rotation, the reuse grace window and reuse detection against Redis. */
@Testcontainers
class RefreshTokenServiceRedisTest {

    private static final long REFRESH_TTL_MS = 60_000;

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redis;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void tokenPresentedAgainWithinGraceGetsTheSameReplacement() {
        RefreshTokenService service = service(30_000);
        User user = user();
        String original = service.issue(user, "session-1");

        RefreshTokenService.Rotation first = service.rotate(original);
        RefreshTokenService.Rotation second = service.rotate(original);

        assertThat(second.refreshToken()).isEqualTo(first.refreshToken());
        assertThat(second.userId()).isEqualTo(user.getId());
        assertThat(second.sessionId()).isEqualTo("session-1");
        // The session stays valid: the shared replacement rotates normally.
        assertThat(service.rotate(first.refreshToken()).refreshToken()).isNotBlank();
    }

    @Test
    void graceTokenIsRefusedWithoutRevokingOnceItsReplacementWasRotated() {
        RefreshTokenService service = service(30_000);
        String original = service.issue(user(), "session-1");
        String replacement = service.rotate(original).refreshToken();
        String latest = service.rotate(replacement).refreshToken();

        assertThatThrownBy(() -> service.rotate(original))
                .isInstanceOf(RefreshTokenSupersededException.class);
        assertThat(service.rotate(latest).refreshToken()).isNotBlank();
    }

    @Test
    void reuseWithoutGraceRevokesTheSession() {
        RefreshTokenService service = service(0);
        User user = user();
        redis.opsForValue().set("user:session:" + user.getId(), "session-1");
        String original = service.issue(user, "session-1");
        String replacement = service.rotate(original).refreshToken();

        assertThatThrownBy(() -> service.rotate(original))
                .isInstanceOf(RefreshTokenReuseException.class);
        assertThatThrownBy(() -> service.rotate(replacement))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(redis.hasKey("user:session:" + user.getId())).isFalse();
    }

    @Test
    void unknownTokenIsInvalid() {
        assertThatThrownBy(() -> service(30_000).rotate("not-a-real-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void graceLongerThanTheRefreshLifetimeIsRejectedAtStartup() {
        assertThatThrownBy(() -> service(REFRESH_TTL_MS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REFRESH_TOKEN_REUSE_GRACE_MS");
    }

    private RefreshTokenService service(long graceMs) {
        JwtProperties properties = new JwtProperties(
                null, "RS256", null, "issuer", "audience", 900_000, REFRESH_TTL_MS, graceMs,
                "ACCESS_TOKEN", "REFRESH_TOKEN", null, false, "Lax");
        return new RefreshTokenService(properties, redis);
    }

    private static User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        return user;
    }
}
