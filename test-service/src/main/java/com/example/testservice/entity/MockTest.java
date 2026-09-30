package com.example.testservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mock_tests")
@Getter
@Setter
@SQLRestriction("deleted_at IS NULL")
public class MockTest extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private TestSeries series;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @Column(name = "total_marks", precision = 10, scale = 2)
    private BigDecimal totalMarks = BigDecimal.ZERO;

    @Column(name = "is_section_order_strict")
    private boolean isSectionOrderStrict;

    @Column(name = "shuffle_sections")
    private boolean shuffleSections;

    @Column(name = "negative_marking_enabled")
    private boolean negativeMarkingEnabled;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "is_free")
    private boolean isFree;

    @Column(name = "published_at")
    private Instant publishedAt;

    @OneToMany(mappedBy = "mockTest", cascade = CascadeType.ALL, orphanRemoval = true)
    @jakarta.persistence.OrderBy("sequenceOrder ASC")
    @SQLRestriction("deleted_at IS NULL")
    private List<Section> sections = new ArrayList<>();
}