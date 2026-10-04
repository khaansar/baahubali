package com.example.attemptservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A lock table whose unique user/test key makes the active-attempt invariant a
 * database guarantee rather than a best-effort Java check.
 */
@Entity
@Table(name = "active_attempts")
@IdClass(ActiveAttemptId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActiveAttempt {

    @Id
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Id
    @Column(name = "test_id", nullable = false, length = 64)
    private String testId;

    @Column(name = "attempt_id", nullable = false, unique = true, length = 36)
    private String attemptId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
