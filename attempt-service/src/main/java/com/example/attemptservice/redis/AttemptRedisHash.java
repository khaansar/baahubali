package com.example.attemptservice.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;

@RedisHash("attempt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptRedisHash implements Serializable {

    @Id
    private String attemptId;

    @Indexed
    private String userId;

    private String examId;
    private Long startedAt;
    private Integer durationSec;
    private String status;

    /** Critical for the write-behind worker: flips true on every PATCH. */
    private Boolean dirtyFlag;

    private Integer currentQuestionIndex;

    private String currentSectionId;
    private String sectionTimeSpentJson;
    private Long currentSectionStartedAt;
    private String sectionDurationsJson;

    /** JSON-serialized map of questionId -> selectedOption, e.g. {"q1":"B"} */
    private String answersJson;

    /** Increments on every write; used for optimistic locking on PATCH. */
    private Long version;

    @TimeToLive
    private Long ttlSeconds;
}