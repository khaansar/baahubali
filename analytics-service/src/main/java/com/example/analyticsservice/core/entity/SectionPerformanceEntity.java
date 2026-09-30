package com.example.analyticsservice.core.entity;

import com.example.analyticsservice.core.exception.*;

import com.example.analyticsservice.core.entity.*;
import com.example.analyticsservice.core.repository.*;

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
@Table(name = "section_performance")
@Getter
@Setter
@NoArgsConstructor
public class SectionPerformanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "attempt_id", length = 36, nullable = false)
    private String attemptId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "test_id", length = 36, nullable = false)
    private String testId;

    @Column(name = "section_id", length = 36, nullable = false)
    private String sectionId;

    @Column(name = "section_name", length = 100, nullable = false)
    private String sectionName;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "correct_count", nullable = false)
    private Integer correctCount;

    @Column(name = "incorrect_count", nullable = false)
    private Integer incorrectCount;

    @Column(name = "unattempted_count", nullable = false)
    private Integer unattemptedCount;

    @Column(name = "accuracy_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal accuracyPercentage;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    /** Reserved for future topic-level analytics; intentionally unused now. */
    @Column(name = "topic_name", length = 100)
    private String topicName;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}