package com.example.analyticsservice.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.analyticsservice.contract.AttemptSubmittedEvent;
import com.example.analyticsservice.contract.PeerComparisonResult;
import com.example.analyticsservice.contract.SectionAnswerPayload;
import java.util.ArrayList;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Exercises the Lua ranking script against an actual Redis server. */
@Testcontainers
class RedisRankingEngineRedisTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redis;
    private RedisRankingEngine engine;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        engine = new RedisRankingEngine(redis);
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) connectionFactory.destroy();
    }

    @Test
    void firstAttemptCreatesLeaderboardInRedis() {
        String testId = testId();

        PeerComparisonResult result = engine.processRankAndStats(attempt("first", testId, 27, 900));

        assertThat(result.rank()).isEqualTo(1);
        assertThat(result.totalParticipants()).isEqualTo(1);
        assertThat(result.averageScore()).isEqualTo(27.0);
        assertThat(redis.opsForZSet().size("analytics:leaderboard:test:" + testId)).isEqualTo(1L);
    }

    @Test
    void normalRankingReturnsOverallStatistics() {
        String testId = testId();
        engine.processRankAndStats(attempt("top", testId, 90, 700));

        PeerComparisonResult result = engine.processRankAndStats(attempt("current", testId, 60, 500));

        assertThat(result.rank()).isEqualTo(2);
        assertThat(result.totalParticipants()).isEqualTo(2);
        assertThat(result.topperScore()).isEqualTo(90.0);
        assertThat(result.topperTimeTakenSeconds()).isEqualTo(700L);
        assertThat(result.averageScore()).isEqualTo(75.0);
        assertThat(result.percentile()).isEqualTo(0.0);
    }

    @Test
    void fasterAttemptWinsWhenScoresTie() {
        String testId = testId();
        engine.processRankAndStats(attempt("slower", testId, 75, 800));

        PeerComparisonResult result = engine.processRankAndStats(attempt("faster", testId, 75, 650));

        assertThat(result.rank()).isEqualTo(1);
        assertThat(result.topperTimeTakenSeconds()).isEqualTo(650L);
    }

    @Test
    void repeatedAttemptIdIsIdempotentInRedis() {
        String testId = testId();
        engine.processRankAndStats(attempt("same-attempt", testId, 25, 1000));

        PeerComparisonResult result = engine.processRankAndStats(attempt("same-attempt", testId, 99, 1));

        assertThat(result.totalParticipants()).isEqualTo(1);
        assertThat(result.topperScore()).isEqualTo(25.0);
        assertThat(result.topperTimeTakenSeconds()).isEqualTo(1000L);
        assertThat(redis.opsForZSet().size("analytics:leaderboard:test:" + testId)).isEqualTo(1L);
    }

    @Test
    void concurrentAttemptsAndDuplicateDeliveryRemainConsistent() throws Exception {
        String testId = testId();
        int submissions = 24;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < submissions; i++) {
                int index = i;
                futures.add(CompletableFuture.runAsync(
                        () -> engine.processRankAndStats(attempt("attempt-" + index, testId, index, 1000 - index)),
                        executor));
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get(20, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        PeerComparisonResult result = engine.processRankAndStats(
                attempt("attempt-0", testId, 0, 1000));

        assertThat(result.totalParticipants()).isEqualTo(submissions);
        assertThat(result.rank()).isEqualTo(submissions);
        assertThat(result.topperScore()).isEqualTo((double) submissions - 1);
        assertThat(redis.opsForZSet().size("analytics:leaderboard:test:" + testId)).isEqualTo((long) submissions);
    }

    private static String testId() {
        return "redis-integration-" + UUID.randomUUID();
    }

    private static AttemptSubmittedEvent attempt(String attemptId, String testId, double score, long time) {
        return new AttemptSubmittedEvent("event-" + attemptId, attemptId, "same-user", "category", "series", testId, time,
                List.of(new SectionAnswerPayload("section", "Section", score, 100.0,
                        1, 1, 0, 0, 100.0, time)), Instant.parse("2026-01-01T00:00:00Z"));
    }
}
