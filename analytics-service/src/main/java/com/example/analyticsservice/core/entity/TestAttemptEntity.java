package com.example.analyticsservice.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "test_attempts")
@Getter
@Setter
@NoArgsConstructor
public class TestAttemptEntity {

    @Id
    @Column(name = "attempt_id", length = 36, nullable = false, updatable = false)
    private String attemptId;

    @Column(name = "test_id", length = 64, nullable = false)
    private String testId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
