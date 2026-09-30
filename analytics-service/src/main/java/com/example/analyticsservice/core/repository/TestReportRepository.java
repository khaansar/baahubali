package com.example.analyticsservice.core.repository;

import com.example.analyticsservice.core.exception.*;

import com.example.analyticsservice.core.entity.*;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestReportRepository extends JpaRepository<TestReportEntity, String> {

    Optional<TestReportEntity> findByAttemptId(String attemptId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from TestReportEntity r where r.attemptId = :attemptId")
    Optional<TestReportEntity> findByAttemptIdForUpdate(@Param("attemptId") String attemptId);
}