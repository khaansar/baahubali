package com.example.analyticsservice.core.repository;

import com.example.analyticsservice.core.entity.TestAttemptEntity;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestAttemptRepository extends JpaRepository<TestAttemptEntity, String> {

    @Modifying
    @Query(value = "INSERT INTO test_attempts (attempt_id, test_id, user_id, started_at) VALUES (:attemptId, :testId, :userId, :startedAt) ON DUPLICATE KEY UPDATE started_at = LEAST(started_at, VALUES(started_at))", nativeQuery = true)
    void upsertFirstAttemptRecord(@Param("attemptId") String attemptId, @Param("testId") String testId, @Param("userId") String userId, @Param("startedAt") LocalDateTime startedAt);
}
