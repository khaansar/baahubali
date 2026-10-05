package com.example.analyticsservice.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "user_topic_performance",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_topic",
                columnNames = {"user_id", "topic"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class UserTopicPerformanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "topic", length = 100, nullable = false)
    private String topic;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions = 0;

    @Column(name = "correct_count", nullable = false)
    private Integer correctCount = 0;

    @Column(name = "incorrect_count", nullable = false)
    private Integer incorrectCount = 0;

    @Column(name = "unattempted_count", nullable = false)
    private Integer unattemptedCount = 0;

    @Column(name = "accuracy_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal accuracyPercentage = BigDecimal.ZERO;

    @Column(name = "attempts_count", nullable = false)
    private Integer attemptsCount = 0;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
