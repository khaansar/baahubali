package com.example.analyticsservice.core.service;

import com.example.analyticsservice.contract.AttemptSubmittedEvent;
import com.example.analyticsservice.contract.SectionAnalysisEngine;
import com.example.analyticsservice.contract.SectionAnalysisResult;
import com.example.analyticsservice.contract.SectionAnswerPayload;
import com.example.analyticsservice.contract.SectionPerformanceResult;
import com.example.analyticsservice.core.exception.InvalidAttemptEventException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Calculates section and overall score summaries from a submitted attempt. */
@Component
public class DefaultSectionAnalysisEngine implements SectionAnalysisEngine {

    @Override
    public SectionAnalysisResult analyze(AttemptSubmittedEvent event) {
        if (event == null || event.sectionAnswers() == null) {
            throw new InvalidAttemptEventException("AttemptSubmittedEvent and sectionAnswers are required");
        }

        List<SectionPerformanceResult> sections = new ArrayList<>(event.sectionAnswers().size());
        double totalScore = 0;
        double maxScore = 0;
        int totalQuestions = 0;
        int correct = 0;

        for (int i = 0; i < event.sectionAnswers().size(); i++) {
            SectionAnswerPayload answer = event.sectionAnswers().get(i);
            validate(answer, i);
            sections.add(new SectionPerformanceResult(
                    answer.sectionId(), answer.sectionName(), answer.score(), answer.maxScore(),
                    answer.totalQuestions(), answer.correct(), answer.incorrect(), answer.unattempted(),
                    answer.accuracy(), answer.timeSpentSeconds()));
            totalScore += answer.score();
            maxScore += answer.maxScore();
            totalQuestions += answer.totalQuestions();
            correct += answer.correct();
        }

        double overallAccuracy = totalQuestions == 0 ? 0 : percentage(correct, totalQuestions);
        return new SectionAnalysisResult(sections, totalScore, maxScore, overallAccuracy);
    }

    private static void validate(SectionAnswerPayload answer, int index) {
        String prefix = "sectionAnswers[" + index + "]";
        if (answer == null) {
            throw new InvalidAttemptEventException(prefix + " is null");
        }
        requireNonNegative(answer.score(), prefix + ".score");
        requireNonNegative(answer.maxScore(), prefix + ".maxScore");
        requireNonNegative(answer.accuracy(), prefix + ".accuracy");
        if (answer.score() > answer.maxScore()) {
            throw new InvalidAttemptEventException(prefix + ".score exceeds maxScore");
        }
        if (answer.accuracy() > 100) {
            throw new InvalidAttemptEventException(prefix + ".accuracy must be between 0 and 100");
        }
        if (answer.totalQuestions() == null || answer.correct() == null
                || answer.incorrect() == null || answer.unattempted() == null) {
            throw new InvalidAttemptEventException(prefix + " question counts are required");
        }
        if (answer.totalQuestions() < 0 || answer.correct() < 0
                || answer.incorrect() < 0 || answer.unattempted() < 0) {
            throw new InvalidAttemptEventException(prefix + " question counts must be non-negative");
        }
        if ((long) answer.correct() + answer.incorrect() + answer.unattempted() != answer.totalQuestions()) {
            throw new InvalidAttemptEventException(prefix + " question counts do not add up to totalQuestions");
        }
        if (answer.timeSpentSeconds() == null || answer.timeSpentSeconds() < 0) {
            throw new InvalidAttemptEventException(prefix + ".timeSpentSeconds must be non-negative");
        }
        if (answer.totalQuestions() == 0 && (answer.score() != 0 || answer.maxScore() != 0 || answer.accuracy() != 0)) {
            throw new InvalidAttemptEventException(prefix + " scores and accuracy must be zero with no questions");
        }
        double expectedAccuracy = answer.totalQuestions() == 0
                ? 0 : percentage(answer.correct(), answer.totalQuestions());
        if (Math.abs(answer.accuracy() - expectedAccuracy) > 0.01) {
            throw new InvalidAttemptEventException(prefix + ".accuracy is inconsistent with question counts");
        }
    }

    private static void requireNonNegative(Double value, String name) {
        if (value == null || !Double.isFinite(value) || value < 0) {
            throw new InvalidAttemptEventException(name + " must be a finite non-negative value");
        }
    }

    private static double percentage(int numerator, int denominator) {
        return Math.round((numerator * 10000.0) / denominator) / 100.0;
    }
}
