package com.example.analyticsservice.core.service;

import com.example.analyticsservice.contract.AttemptSubmittedEvent;
import com.example.analyticsservice.contract.PeerComparisonResult;
import com.example.analyticsservice.contract.RankingEngine;
import com.example.analyticsservice.contract.SectionAnswerPayload;
import com.example.analyticsservice.core.exception.InvalidAttemptEventException;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/** Maintains a per-test attempt leaderboard and calculates peer statistics. */
@Component
public class RedisRankingEngine implements RankingEngine {

    private static final String LEADERBOARD_KEY_PREFIX = "analytics:leaderboard:test:";
    private static final String TIME_HASH_SUFFIX = ":times";
    private static final String STATS_HASH_SUFFIX = ":stats";

    private static final RedisScript<List> UPSERT_AND_READ = new DefaultRedisScript<>("""
            local leaderboard = KEYS[1]
            local times = KEYS[2]
            local stats = KEYS[3]
            local attemptId = ARGV[1]
            local score = ARGV[2]
            local timeTaken = ARGV[3]

            local storedScore = redis.call('ZSCORE', leaderboard, attemptId)
            if not storedScore then
              redis.call('ZADD', leaderboard, score, attemptId)
              redis.call('HSET', times, attemptId, timeTaken)
              redis.call('HINCRBY', stats, 'participants', 1)
              redis.call('HINCRBYFLOAT', stats, 'scoreTotal', score)
              storedScore = score
            end

            local currentTime = tonumber(redis.call('HGET', times, attemptId))
            local currentScoreNumber = tonumber(storedScore)
            local rank = redis.call('ZCOUNT', leaderboard, '(' .. storedScore, '+inf') + 1
            local equalScoreAttempts = redis.call('ZRANGEBYSCORE', leaderboard, storedScore, storedScore)

            for _, member in ipairs(equalScoreAttempts) do
              if member ~= attemptId then
                local otherTime = tonumber(redis.call('HGET', times, member))
                if otherTime < currentTime or (otherTime == currentTime and member < attemptId) then
                  rank = rank + 1
                end
              end
            end

            local participants = tonumber(redis.call('HGET', stats, 'participants'))
            local scoreTotal = tonumber(redis.call('HGET', stats, 'scoreTotal'))
            local topper = redis.call('ZREVRANGE', leaderboard, 0, 0, 'WITHSCORES')
            local topperScore = topper[2]
            local topperAttempts = redis.call('ZRANGEBYSCORE', leaderboard, topperScore, topperScore)
            local topperId = topperAttempts[1]
            local topperTime = tonumber(redis.call('HGET', times, topperId))

            for index = 2, #topperAttempts do
              local candidateId = topperAttempts[index]
              local candidateTime = tonumber(redis.call('HGET', times, candidateId))
              if candidateTime < topperTime or (candidateTime == topperTime and candidateId < topperId) then
                topperId = candidateId
                topperTime = candidateTime
              end
            end

            local percentile = participants == 1 and 100 or ((participants - rank) * 100 / (participants - 1))

            return {
              tostring(rank),
              tostring(participants),
              string.format('%.17g', scoreTotal / participants),
              topperScore,
              tostring(topperTime),
              string.format('%.10f', percentile)
            }
            """, List.class);

    private final StringRedisTemplate redis;

    public RedisRankingEngine(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public PeerComparisonResult processRankAndStats(AttemptSubmittedEvent event) {
        validate(event);

        double score = event.sectionAnswers()
                .stream()
                .mapToDouble(SectionAnswerPayload::score)
                .sum();

        long timeTaken = event.timeTakenSeconds();
        String leaderboardKey = LEADERBOARD_KEY_PREFIX + event.testId();

        List<?> result = redis.execute(
                UPSERT_AND_READ,
                List.of(
                        leaderboardKey,
                        leaderboardKey + TIME_HASH_SUFFIX,
                        leaderboardKey + STATS_HASH_SUFFIX
                ),
                event.attemptId(),
                Double.toString(score),
                Long.toString(timeTaken)
        );

        if (result == null || result.size() != 6) {
            throw new IllegalStateException("Redis returned an invalid leaderboard result");
        }

        return new PeerComparisonResult(
                Double.parseDouble(result.get(3).toString()),
                Double.parseDouble(result.get(2).toString()),
                Long.parseLong(result.get(4).toString()),
                Integer.parseInt(result.get(0).toString()),
                Integer.parseInt(result.get(1).toString()),
                Double.parseDouble(result.get(5).toString())
        );
    }

    private static void validate(AttemptSubmittedEvent event) {
        if (event == null) {
            throw new InvalidAttemptEventException("AttemptSubmittedEvent is required");
        }

        if (isBlank(event.attemptId())) {
            throw new InvalidAttemptEventException("attemptId is required");
        }

        if (isBlank(event.testId())) {
            throw new InvalidAttemptEventException("testId is required");
        }

        if (event.timeTakenSeconds() == null || event.timeTakenSeconds() < 0) {
            throw new InvalidAttemptEventException("timeTakenSeconds must be non-negative");
        }

        if (event.sectionAnswers() == null) {
            throw new InvalidAttemptEventException("sectionAnswers are required to calculate score");
        }

        for (SectionAnswerPayload answer : event.sectionAnswers()) {
            if (answer == null
                    || answer.score() == null
                    || !Double.isFinite(answer.score())) {
                throw new InvalidAttemptEventException(
                        "section answer score must be finite"
                );
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}