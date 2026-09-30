package com.example.analyticsservice.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.analyticsservice.contract.AttemptSubmittedEvent;
import com.example.analyticsservice.contract.PeerComparisonResult;
import com.example.analyticsservice.contract.SectionAnswerPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class RedisRankingEngineTest {

    private final Map<String, Map<String, Entry>> leaderboardStore = new HashMap<>();
    private StringRedisTemplate redis;
    private RedisRankingEngine engine;

    @BeforeEach
    void setUp() {
        leaderboardStore.clear();
        redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    List<String> keys = invocation.getArgument(1);
                    String leaderboardKey = keys.get(0);
                    String attemptId = invocation.getArgument(2).toString();
                    double score = Double.parseDouble(invocation.getArgument(3).toString());
                    long time = Long.parseLong(invocation.getArgument(4).toString());

                    synchronized (leaderboardStore) {
                        Map<String, Entry> attempts = leaderboardStore.computeIfAbsent(
                                leaderboardKey, ignored -> new HashMap<>());
                        attempts.putIfAbsent(attemptId, new Entry(attemptId, score, time));
                        List<Entry> ranked = attempts.values().stream()
                                .sorted(Comparator.comparingDouble(Entry::score).reversed()
                                        .thenComparingLong(Entry::time)
                                        .thenComparing(Entry::attemptId))
                                .toList();
                        int rank = 1 + ranked.indexOf(attempts.get(attemptId));
                        Entry topper = ranked.get(0);
                        double scoreTotal = ranked.stream().mapToDouble(Entry::score).sum();
                        double percentile = ranked.size() == 1 ? 100.0
                                : ((ranked.size() - rank) * 100.0) / (ranked.size() - 1);
                        return List.of(Integer.toString(rank), Integer.toString(ranked.size()),
                                Double.toString(scoreTotal / ranked.size()), Double.toString(topper.score()),
                                Long.toString(topper.time()), Double.toString(percentile));
                    }
                });
        engine = new RedisRankingEngine(redis);
    }

    @Test
    void firstAttemptCreatesAnEmptyLeaderboardAndGetsRankOne() {
        PeerComparisonResult result = engine.processRankAndStats(attempt("first", "test", 27, 900));

        assertThat(result.rank()).isEqualTo(1);
        assertThat(result.totalParticipants()).isEqualTo(1);
        assertThat(result.topperScore()).isEqualTo(27.0);
        assertThat(result.averageScore()).isEqualTo(27.0);
        assertThat(result.topperTimeTakenSeconds()).isEqualTo(900L);
        assertThat(result.percentile()).isEqualTo(100.0);
    }

    @Test
    void higherScoreRanksFirstAndAverageAndTopperStatsAreCalculated() {
        engine.processRankAndStats(attempt("top", "test", 90, 700));

        PeerComparisonResult result = engine.processRankAndStats(attempt("current", "test", 60, 500));

        assertThat(result.rank()).isEqualTo(2);
        assertThat(result.totalParticipants()).isEqualTo(2);
        assertThat(result.topperScore()).isEqualTo(90.0);
        assertThat(result.averageScore()).isEqualTo(75.0);
        assertThat(result.topperTimeTakenSeconds()).isEqualTo(700L);
        assertThat(result.percentile()).isEqualTo(0.0);
    }

    @Test
    void fasterAttemptWinsWhenScoresTie() {
        engine.processRankAndStats(attempt("slower", "test", 75, 800));

        PeerComparisonResult result = engine.processRankAndStats(attempt("faster", "test", 75, 650));

        assertThat(result.rank()).isEqualTo(1);
        assertThat(result.topperTimeTakenSeconds()).isEqualTo(650L);
        assertThat(result.percentile()).isEqualTo(100.0);
    }

    @Test
    void duplicateAttemptIdDoesNotChangeScoreOrParticipantCount() {
        engine.processRankAndStats(attempt("same-attempt", "test", 25, 1000));

        PeerComparisonResult duplicate = engine.processRankAndStats(attempt("same-attempt", "test", 99, 1));

        assertThat(duplicate.totalParticipants()).isEqualTo(1);
        assertThat(duplicate.topperScore()).isEqualTo(25.0);
        assertThat(duplicate.topperTimeTakenSeconds()).isEqualTo(1000L);
    }

    @Test
    void concurrentSubmissionsAreAllPresentAndSameAttemptRemainsIdempotent() throws Exception {
        int submissions = 24;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < submissions; i++) {
                int index = i;
                futures.add(CompletableFuture.runAsync(
                        () -> engine.processRankAndStats(attempt("attempt-" + index, "concurrent", index, 1000 - index)),
                        executor));
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        PeerComparisonResult result = engine.processRankAndStats(
                attempt("attempt-0", "concurrent", 0, 1000));

        assertThat(result.totalParticipants()).isEqualTo(submissions);
        assertThat(result.rank()).isEqualTo(submissions);
        assertThat(result.topperScore()).isEqualTo((double) submissions - 1);
    }

    @Test
    void negativeScoreFromNegativeMarkingIsAccepted() {
        AttemptSubmittedEvent event = new AttemptSubmittedEvent(
                "event-negative",
                "attempt-negative",
                "same-user",
                "category",
                "series",
                "test",
                300L,
                List.of(new SectionAnswerPayload(
                        "section",
                        "Section",
                        -2.0,
                        10.0,
                        1,
                        0,
                        1,
                        0,
                        0.0,
                        300L
                )),
                Instant.parse("2026-01-01T00:00:00Z")
        );

        PeerComparisonResult result = engine.processRankAndStats(event);

        assertThat(result.rank()).isEqualTo(1);
        assertThat(result.totalParticipants()).isEqualTo(1);
        assertThat(result.topperScore()).isEqualTo(-2.0);
        assertThat(result.averageScore()).isEqualTo(-2.0);
        assertThat(result.percentile()).isEqualTo(100.0);
    }

    private static AttemptSubmittedEvent attempt(String attemptId, String testId, double score, long time) {
        return new AttemptSubmittedEvent("event-" + attemptId, attemptId, "same-user", "category", "series", testId, time,
                List.of(new SectionAnswerPayload("section", "Section", score, 100.0,
                        1, 1, 0, 0, 100.0, time)), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private record Entry(String attemptId, double score, long time) { }
}
