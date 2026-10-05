package com.example.attemptservice.repository;

import com.example.attemptservice.entity.ActiveAttempt;
import com.example.attemptservice.entity.ActiveAttemptId;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ActiveAttemptRepository extends JpaRepository<ActiveAttempt, ActiveAttemptId> {

    Optional<ActiveAttempt> findByUserIdAndTestId(String userId, String testId);

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO active_attempts (user_id, test_id, attempt_id, created_at)
            VALUES (:userId, :testId, :attemptId, :createdAt)
            """, nativeQuery = true)
    int claim(@Param("userId") String userId,
              @Param("testId") String testId,
              @Param("attemptId") String attemptId,
              @Param("createdAt") Instant createdAt);

    @Modifying
    @Query("DELETE FROM ActiveAttempt a WHERE a.attemptId = :attemptId")
    void releaseByAttemptId(@Param("attemptId") String attemptId);
}
