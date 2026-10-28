package com.example.attemptservice.repository;

import com.example.attemptservice.entity.Attempt;
import com.example.attemptservice.entity.AttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, String> {

    List<Attempt> findByUserIdOrderByStartedAtDesc(String userId);

    List<Attempt> findByUserIdAndStatusOrderByStartedAtDesc(String userId, AttemptStatus status);

    Optional<Attempt> findFirstByUserIdAndStatusOrderByStartedAtDesc(String userId, AttemptStatus status);

    Page<Attempt> findByUserId(String userId, Pageable pageable);

    List<Attempt> findByStatus(AttemptStatus status);

    Optional<Attempt> findFirstByUserIdAndTestIdAndStatusOrderByStartedAtDesc(
            String userId,
            String testId,
            AttemptStatus status
    );

    boolean existsByUserIdAndTestIdAndStatus(
            String userId,
            String testId,
            AttemptStatus status
    );

    @Query(value = "SELECT * FROM attempts WHERE id = :attemptId FOR UPDATE", nativeQuery = true)
    Optional<Attempt> findByIdForUpdate(@Param("attemptId") String attemptId);

    
    @Query(value = """
            SELECT DISTINCT a.user_id
            FROM attempts a
            WHERE a.status = 'SUBMITTED'
            AND a.updated_at >= :yesterdayStart
            AND a.updated_at < :todayStart
            AND NOT EXISTS (
                SELECT 1
                FROM attempts t
                WHERE t.user_id = a.user_id
                    AND t.status = 'SUBMITTED'
                    AND t.updated_at >= :todayStart
                    AND t.updated_at < :tomorrowStart
            )
            """, nativeQuery = true)
    List<String> findUsersEligibleForStreakReminder(
            @Param("yesterdayStart") Instant yesterdayStart,
            @Param("todayStart") Instant todayStart,
            @Param("tomorrowStart") Instant tomorrowStart
    );
}
