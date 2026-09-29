package com.example.attemptservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Maps to the `attempt_answers` table (architecture doc, section 3, Layer B).
 * Populated by the Write-Behind worker flushing dirty Redis hashes, and
 * directly on submit for the final flush.
 */
@Entity
@Table(
        name = "attempt_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attempt_question",
                columnNames = {"attempt_id", "question_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false, length = 36)
    private String attemptId;

    @Column(name = "question_id", nullable = false, length = 64)
    private String questionId;

    @Column(name = "selected_option", length = 64)
    private String selectedOption;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}