package com.example.attemptservice.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionReviewDto {
    private String questionId;
    private String sectionId;
    private String sectionTitle;
    private String questionText;

    private List<Map<String, Object>> questionImages;
    private Map<String, List<Map<String, Object>>> optionImages;
    private Map<String, Object> options;
    private String questionType;

    private String selectedOption;
    private String correctOption;
    private String status;
    private Double positiveMarks;
    private Double negativeMarks;
    private Double marksAwarded;
    private String explanation;
    private List<Map<String, Object>> explanationImages;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
