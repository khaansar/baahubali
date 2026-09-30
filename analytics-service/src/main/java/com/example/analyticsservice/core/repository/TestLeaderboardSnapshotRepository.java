package com.example.analyticsservice.core.repository;

import com.example.analyticsservice.core.exception.*;

import com.example.analyticsservice.core.entity.*;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestLeaderboardSnapshotRepository extends JpaRepository<TestLeaderboardSnapshotEntity, Long> {

    Optional<TestLeaderboardSnapshotEntity> findByAttemptId(String attemptId);
}