package com.example.analyticsservice.core.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestLeaderboardSnapshotRepository extends JpaRepository<TestLeaderboardSnapshotEntity, Long> {

    Optional<TestLeaderboardSnapshotEntity> findByAttemptId(String attemptId);
}