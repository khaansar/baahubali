package com.example.analyticsservice.core.entity;

import com.example.analyticsservice.core.exception.*;

import com.example.analyticsservice.core.entity.*;
import com.example.analyticsservice.core.repository.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "test_reports")
@Getter
@Setter
@NoArgsConstructor
public class TestReportEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "attempt_id", length = 36, nullable = false, unique = true)
    private String attemptId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "category_id", length = 36, nullable = false)
    private String categoryId;

    @Column(name = "test_series_id", length = 36, nullable = false)
    private String testSeriesId;

    @Column(name = "test_id", length = 36, nullable = false)
    private String testId;

    @Column(name = "total_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "max_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "accuracy_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal accuracyPercentage;

    @Column(name = "time_taken_seconds", nullable = false)
    private Integer timeTakenSeconds;

    /** DB column is named `rank`, which is reserved in MySQL 8, hence the quoting. */
    @Column(name = "`rank`")
    private Integer rankPosition;

    @Column(name = "percentile", precision = 5, scale = 2)
    private BigDecimal percentile;

    /** Stored as a String holding a ReportStatus name (avoids native ENUM mapping on MySQL). */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    /** Serialized ReportData JSON. */
    @Column(name = "report_data", columnDefinition = "json")
    private String reportData;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}