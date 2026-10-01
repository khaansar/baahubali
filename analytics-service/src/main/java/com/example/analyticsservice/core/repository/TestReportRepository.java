package com.example.analyticsservice.core.repository;

import com.example.analyticsservice.core.entity.TestReportEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestReportRepository extends JpaRepository<TestReportEntity, String> {

    Optional<TestReportEntity> findByAttemptId(String attemptId);

    List<TestReportEntity> findByUserIdAndStatusOrderByCreatedAtAsc(
            String userId,
            String status
    );

    @Query(
            value = "SELECT * FROM test_reports WHERE attempt_id = :attemptId FOR UPDATE",
            nativeQuery = true
    )
    Optional<TestReportEntity> findByAttemptIdForUpdate(@Param("attemptId") String attemptId);
}