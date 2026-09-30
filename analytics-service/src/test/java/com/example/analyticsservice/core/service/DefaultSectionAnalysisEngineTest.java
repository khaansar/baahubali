package com.example.analyticsservice.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.analyticsservice.contract.AttemptSubmittedEvent;
import com.example.analyticsservice.contract.SectionAnalysisResult;
import com.example.analyticsservice.contract.SectionAnswerPayload;
import com.example.analyticsservice.core.exception.InvalidAttemptEventException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultSectionAnalysisEngineTest {

    private final DefaultSectionAnalysisEngine engine = new DefaultSectionAnalysisEngine();

    @Test
    void calculatesSectionsAndOverallSummary() {
        SectionAnalysisResult result = engine.analyze(event(
                section("math", 8.0, 10.0, 10, 8, 1, 1, 80.0, 120L),
                section("science", 5.0, 10.0, 10, 5, 3, 2, 50.0, 180L)));

        assertThat(result.sections()).hasSize(2);
        assertThat(result.sections().get(0).sectionId()).isEqualTo("math");
        assertThat(result.totalScore()).isEqualTo(13.0);
        assertThat(result.maxScore()).isEqualTo(20.0);
        assertThat(result.accuracyPercentage()).isEqualTo(65.0);
    }

    @Test
    void handlesEmptySectionsAndZeroQuestions() {
        SectionAnalysisResult empty = engine.analyze(event());
        SectionAnalysisResult zeroQuestions = engine.analyze(event(
                section("empty", 0.0, 0.0, 0, 0, 0, 0, 0.0, 0L)));

        assertThat(empty.sections()).isEmpty();
        assertThat(empty.totalScore()).isZero();
        assertThat(empty.maxScore()).isZero();
        assertThat(empty.accuracyPercentage()).isZero();
        assertThat(zeroQuestions.accuracyPercentage()).isZero();
    }

    @Test
    void handlesAllUnattemptedZeroScoreAndPerfectScore() {
        SectionAnalysisResult allUnattempted = engine.analyze(event(
                section("unattempted", 0.0, 12.0, 4, 0, 0, 4, 0.0, 0L)));
        SectionAnalysisResult perfect = engine.analyze(event(
                section("perfect", 10.0, 10.0, 5, 5, 0, 0, 100.0, 30L)));

        assertThat(allUnattempted.totalScore()).isZero();
        assertThat(allUnattempted.accuracyPercentage()).isZero();
        assertThat(perfect.totalScore()).isEqualTo(10.0);
        assertThat(perfect.accuracyPercentage()).isEqualTo(100.0);
    }

    @Test
    void rejectsInvalidCountsAndNegativeTime() {
        assertThatThrownBy(() -> engine.analyze(event(
                section("bad-counts", 1.0, 2.0, 3, 1, 1, 0, 33.33, 1L))))
                .isInstanceOf(InvalidAttemptEventException.class)
                .hasMessageContaining("do not add up");
        assertThatThrownBy(() -> engine.analyze(event(
                section("bad-time", 1.0, 2.0, 1, 1, 0, 0, 100.0, -1L))))
                .isInstanceOf(InvalidAttemptEventException.class)
                .hasMessageContaining("timeSpentSeconds");
    }

    @Test
    void rejectsNegativeScoresAndInconsistentAccuracy() {
        assertThatThrownBy(() -> engine.analyze(event(
                section("negative", -1.0, 2.0, 1, 1, 0, 0, 100.0, 1L))))
                .isInstanceOf(InvalidAttemptEventException.class)
                .hasMessageContaining("score");
        assertThatThrownBy(() -> engine.analyze(event(
                section("bad-accuracy", 1.0, 2.0, 2, 1, 1, 0, 100.0, 1L))))
                .isInstanceOf(InvalidAttemptEventException.class)
                .hasMessageContaining("accuracy");
    }

    private static AttemptSubmittedEvent event(SectionAnswerPayload... sections) {
        return new AttemptSubmittedEvent("event-attempt", "attempt", "user", "category", "series", "test", 10L,
                List.of(sections), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static SectionAnswerPayload section(String id, double score, double maxScore,
                                                 int total, int correct, int incorrect,
                                                 int unattempted, double accuracy, long time) {
        return new SectionAnswerPayload(id, id, score, maxScore, total, correct, incorrect,
                unattempted, accuracy, time);
    }
}
