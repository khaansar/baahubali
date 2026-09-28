package com.example.attemptservice.worker;

import com.example.attemptservice.entity.AttemptAnswer;
import com.example.attemptservice.redis.AttemptRedisHash;
import com.example.attemptservice.redis.AttemptRedisRepository;
import com.example.attemptservice.repository.AttemptAnswerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttemptFlushWorker {

    private final AttemptRedisRepository attemptRedisRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

                    // Mark as clean and update in Redis
                    hash.setDirtyFlag(false);
                    attemptRedisRepository.save(hash);

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

    public void flushAnswers(AttemptRedisHash hash) throws Exception {
        if (hash.getAnswersJson() == null || hash.getAnswersJson().isEmpty()) {
            return;
        }

        Map<String, String> answersMap = objectMapper.readValue(
                hash.getAnswersJson(),
                new TypeReference<Map<String, String>>() {}
        );

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
            ans.setUpdatedAt(now);
            toSave.add(ans);
        }

        if (!toSave.isEmpty()) {
            attemptAnswerRepository.saveAll(toSave);

            log.debug(
                    "Flushed {} answers for attempt {}",
                    toSave.size(),
                    hash.getAttemptId()
            );
        }
    }
}