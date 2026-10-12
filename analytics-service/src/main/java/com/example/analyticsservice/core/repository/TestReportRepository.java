package com.example.analyticsservice.core.repository;

import com.example.analyticsservice.contract.TestLeaderboardRow;
import com.example.analyticsservice.core.entity.TestReportEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestReportRepository extends JpaRepository<TestReportEntity, String> {

    Optional<TestReportEntity> findByAttemptId(String attemptId);

    List<TestReportEntity> findByUserIdAndStatusOrderByCreatedAtAsc(String userId, String status);

    @Query(value = "SELECT * FROM test_reports WHERE attempt_id = :attemptId FOR UPDATE", nativeQuery = true)
    Optional<TestReportEntity> findByAttemptIdForUpdate(@Param("attemptId") String attemptId);

    @Query(
            value = """
                    SELECT new com.example.analyticsservice.contract.TestLeaderboardRow(
                        report.userId,
                        report.totalScore,
                        report.maxScore,
                        report.accuracyPercentage,
                        report.timeTakenSeconds,
                        attempt.startedAt
                    )
                    FROM TestReportEntity report, TestAttemptEntity attempt
                    WHERE report.testId = :testId
                      AND report.status = 'COMPLETED'
                      AND attempt.attemptId = report.attemptId
                      AND attempt.testId = report.testId
                      AND attempt.userId = report.userId
                      AND NOT EXISTS (
                          SELECT earlier.attemptId
                          FROM TestAttemptEntity earlier
                          WHERE earlier.testId = attempt.testId
                            AND earlier.userId = attempt.userId
                            AND (earlier.startedAt < attempt.startedAt OR (earlier.startedAt = attempt.startedAt AND earlier.attemptId < attempt.attemptId))
                      )
                    ORDER BY report.totalScore DESC, report.timeTakenSeconds ASC, attempt.startedAt ASC, report.userId ASC
                    """,
            countQuery = """
                    SELECT COUNT(report.id)
                    FROM TestReportEntity report, TestAttemptEntity attempt
                    WHERE report.testId = :testId
                      AND report.status = 'COMPLETED'
                      AND attempt.attemptId = report.attemptId
                      AND attempt.testId = report.testId
                      AND attempt.userId = report.userId
                      AND NOT EXISTS (
                          SELECT earlier.attemptId
                          FROM TestAttemptEntity earlier
                          WHERE earlier.testId = attempt.testId
                            AND earlier.userId = attempt.userId
                            AND (earlier.startedAt < attempt.startedAt OR (earlier.startedAt = attempt.startedAt AND earlier.attemptId < attempt.attemptId))
                      )
                    """
    )
    Page<TestLeaderboardRow> findFirstAttemptLeaderboard(@Param("testId") String testId, Pageable pageable);
}
