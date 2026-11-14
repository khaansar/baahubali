package com.example.testservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "section_questions")
@Getter
@Setter
public class SectionQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "positive_marks_override", precision = 10, scale = 2)
    private BigDecimal positiveMarksOverride;

    @Column(name = "negative_marks_override", precision = 10, scale = 2)
    private BigDecimal negativeMarksOverride;
}