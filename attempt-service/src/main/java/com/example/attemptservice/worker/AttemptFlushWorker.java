package com.example.attemptservice.worker;

import com.example.attemptservice.entity.AttemptAnswer;
import com.example.attemptservice.redis.AttemptRedisHash;
import com.example.attemptservice.redis.AttemptRedisRepository;
import com.example.attemptservice.repository.AttemptAnswerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttemptFlushWorker {

    /*
     * Clears the dirty flag only if no answer was saved while the flush ran, and touches
     * nothing else. Writing the whole hash back would overwrite answers saved in that window
     * with the copy read before the flush, silently losing answers the student was told were saved.
     */
    static final RedisScript<Long> CLEAR_DIRTY_IF_UNCHANGED = new DefaultRedisScript<>(
            "if redis.call('HGET', KEYS[1], 'version') == ARGV[1] then " +
            "redis.call('HSET', KEYS[1], 'dirtyFlag', '0') return 1 end " +
            "return 0",
            Long.class
    );

    private static final String REDIS_KEY_PREFIX = "attempt:";

    private final AttemptRedisRepository attemptRedisRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final com.example.attemptservice.repository.AttemptRepository attemptRepository;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedDelayString = "15000")
    public void flushDirtyHashes() {
        log.debug("Starting flush of dirty Redis hashes...");
        Iterable<AttemptRedisHash> hashes = attemptRedisRepository.findAll();

        for (AttemptRedisHash hash : hashes) {
            if (hash == null) {
                continue;
            }

            if (Boolean.TRUE.equals(hash.getDirtyFlag())) {
                try {
                    flushAnswers(hash);
                    clearDirtyIfUnchanged(hash);
                } catch (Exception e) {
                    log.error(
                            "Failed to flush dirty hash for attemptId: {}",
                            hash.getAttemptId(),
                            e
                    );
                }
            }
        }
    }

    /** A save during the flush bumps the version, so the hash stays dirty and is flushed next run. */
    private void clearDirtyIfUnchanged(AttemptRedisHash hash) {
        if (hash.getVersion() == null) {
            return;
        }
        redisTemplate.execute(
                CLEAR_DIRTY_IF_UNCHANGED,
                List.of(REDIS_KEY_PREFIX + hash.getAttemptId()),
                hash.getVersion().toString()
        );
    }

    public void flushAnswers(AttemptRedisHash hash) throws Exception {
        if (hash.getAnswersJson() == null || hash.getAnswersJson().isEmpty()) {
            return;
        }

        Map<String, String> answersMap = objectMapper.readValue(
                hash.getAnswersJson(),
                new TypeReference<Map<String, String>>() {}
        );

        Set<String> marked = hash.getMarkedJson() == null || hash.getMarkedJson().isBlank()
                ? Set.of()
                : new HashSet<>(objectMapper.readValue(
                        hash.getMarkedJson(),
                        new TypeReference<List<String>>() {}
                ));

        List<AttemptAnswer> existingAnswers =
                attemptAnswerRepository.findByAttemptId(hash.getAttemptId());

        Map<String, AttemptAnswer> existingByQuestion =
                existingAnswers.stream()
                        .collect(Collectors.toMap(
                                AttemptAnswer::getQuestionId,
                                Function.identity()
                        ));

        List<AttemptAnswer> toSave = new ArrayList<>();
        Instant now = Instant.now();

        for (Map.Entry<String, String> entry : answersMap.entrySet()) {
            String qId = entry.getKey();
            String opt = entry.getValue();

            AttemptAnswer ans = existingByQuestion.get(qId);

            if (ans == null) {
                ans = AttemptAnswer.builder()
                        .attemptId(hash.getAttemptId())
                        .questionId(qId)
                        .build();
            }

            ans.setSelectedOption(opt);
            ans.setMarkedForReview(marked.contains(qId));
            ans.setUpdatedAt(now);
            toSave.add(ans);
        }

        if (!toSave.isEmpty()) {
            attemptAnswerRepository.saveAll(toSave);

            com.example.attemptservice.entity.Attempt attempt = attemptRepository.findById(hash.getAttemptId()).orElse(null);
            if (attempt != null && hash.getSectionTimeSpentJson() != null) {
                attempt.setSectionTimeSpentJson(hash.getSectionTimeSpentJson());
                attemptRepository.save(attempt);
            }

            log.debug(
                    "Flushed {} answers for attempt {}",
                    toSave.size(),
                    hash.getAttemptId()
            );
        }
    }
}