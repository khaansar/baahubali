package com.example.testservice.entity;

import com.example.testservice.entity.converter.MapStringJsonConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "questions")
@Getter
@Setter
@SQLRestriction("deleted_at IS NULL")
public class Question extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, updatable = false)
    private QuestionType questionType;

    @Convert(converter = MapStringJsonConverter.class)
    @Column(name = "correct_answer_json", columnDefinition = "JSON", nullable = false)
    private Map<String, Object> correctAnswerJson;

    @Column(name = "positive_marks", precision = 10, scale = 2, nullable = false)
    private BigDecimal positiveMarks;

    @Column(name = "negative_marks", precision = 10, scale = 2, nullable = false)
    private BigDecimal negativeMarks;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "topic", length = 100)
    private String topic;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Column(name = "is_locked", nullable = false)
    private boolean isLocked = false;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SectionQuestion> sectionQuestions = new ArrayList<>();

    // Added Translations Mapping
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionTranslation> translations = new ArrayList<>();
}