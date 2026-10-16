package com.example.testservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "question_translations")
@Getter
@Setter
@SQLRestriction("deleted_at IS NULL")
public class QuestionTranslation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private String language;

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Column(name = "options_json", columnDefinition = "json")
    private String optionsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_images", columnDefinition = "json")
    private List<Map<String, Object>> questionImages;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "option_images", columnDefinition = "json")
    private Map<String, List<Map<String, Object>>> optionImages;
}