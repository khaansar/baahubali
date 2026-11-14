package com.example.iam.service;

import com.example.iam.config.OtpProperties;
import com.example.iam.exception.OtpCooldownException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Exercises the OTP Lua script and cooldown against an actual Redis server. */
@Testcontainers
class OtpServiceRedisTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        otpService = new OtpService(redis, new OtpProperties(Duration.ofMinutes(10), 3, Duration.ofSeconds(60)));
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void codeIsSingleUseAndScopedToPurpose() {
        UUID userId = UUID.randomUUID();
        String code = otpService.issue(OtpService.Purpose.EMAIL_VERIFICATION, userId);

        assertThat(otpService.verify(OtpService.Purpose.ACCOUNT_DELETION, userId, code)).isFalse();
        assertThat(otpService.verify(OtpService.Purpose.EMAIL_VERIFICATION, userId, code)).isTrue();
        assertThat(otpService.verify(OtpService.Purpose.EMAIL_VERIFICATION, userId, code)).isFalse();
    }

    @Test
    void codeIsDiscardedAfterTheAllowedWrongAttempts() {
        UUID userId = UUID.randomUUID();
        String code = otpService.issue(OtpService.Purpose.EMAIL_VERIFICATION, userId);
        String wrong = code.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 3; i++) {
            assertThat(otpService.verify(OtpService.Purpose.EMAIL_VERIFICATION, userId, wrong)).isFalse();
        }

        assertThat(otpService.verify(OtpService.Purpose.EMAIL_VERIFICATION, userId, code)).isFalse();
    }

    @Test
    void secondCodeWithinCooldownIsRefused() {
        UUID userId = UUID.randomUUID();
        otpService.issue(OtpService.Purpose.ACCOUNT_DELETION, userId);

        assertThatThrownBy(() -> otpService.issue(OtpService.Purpose.ACCOUNT_DELETION, userId))
                .isInstanceOf(OtpCooldownException.class);
    }
}
