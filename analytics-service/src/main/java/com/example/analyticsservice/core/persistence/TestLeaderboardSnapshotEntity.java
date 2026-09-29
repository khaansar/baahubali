package com.example.analyticsservice.core.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "test_leaderboard_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class TestLeaderboardSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "test_id", length = 36, nullable = false)
    private String testId;

    @Column(name = "attempt_id", length = 36, nullable = false, unique = true)
    private String attemptId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "score", nullable = false, precision = 6, scale = 2)
    private BigDecimal score;

    @Column(name = "time_taken_seconds", nullable = false)
    private Integer timeTakenSeconds;

    @Column(name = "`rank`", nullable = false)
    private Integer rankPosition;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}