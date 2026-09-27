package com.example.testservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sections")
@Getter
@Setter
@SQLRestriction("deleted_at IS NULL")
public class Section extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private MockTest mockTest;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "shuffle_questions")
    private boolean shuffleQuestions;

    @Column(name = "default_negative_marks", precision = 5, scale = 2)
    private java.math.BigDecimal defaultNegativeMarks;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    private List<SectionQuestion> sectionQuestions = new ArrayList<>();
}