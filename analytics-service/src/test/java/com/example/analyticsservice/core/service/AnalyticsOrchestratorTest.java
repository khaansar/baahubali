package com.example.analyticsservice.core.service;
import com.example.analyticsservice.core.exception.*;


import com.example.analyticsservice.core.entity.*;
import com.example.analyticsservice.core.repository.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.analyticsservice.contract.AttemptSubmittedEvent;
import com.example.analyticsservice.contract.PeerComparisonResult;
import com.example.analyticsservice.contract.RankingEngine;
import com.example.analyticsservice.contract.SectionAnalysisEngine;
import com.example.analyticsservice.contract.SectionAnalysisResult;
import com.example.analyticsservice.contract.SectionAnswerPayload;
import com.example.analyticsservice.contract.SectionPerformanceResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class AnalyticsOrchestratorTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private SectionAnalysisEngine sectionEngine;
    private RankingEngine rankingEngine;
    private ReportPersistenceService persistence;
    private AnalyticsOrchestrator orchestrator;

    private final AttemptSubmittedEvent event = new AttemptSubmittedEvent(
            "event-1", "att-1", "user-1", "cat-1", "series-1", "test-1", 5420L,
            List.of(new SectionAnswerPayload("s1", "Physics", 65.0, 100.0, 30, 20, 5, 5, 80.0, 1800L)),
            Instant.parse("2026-01-01T00:00:00Z"));

    private final SectionAnalysisResult sectionResult = new SectionAnalysisResult(
            List.of(new SectionPerformanceResult("s1", "Physics", 65.0, 100.0, 30, 20, 5, 5, 80.0, 1800L)),
            185.5, 300.0, 82.5);

    private final PeerComparisonResult peer = new PeerComparisonResult(285.0, 112.4, 4800L, 42, 1500, 97.2);

    @BeforeEach
    void setUp() {
        sectionEngine = mock(SectionAnalysisEngine.class);
        rankingEngine = mock(RankingEngine.class);
        persistence = mock(ReportPersistenceService.class);
        org.springframework.context.ApplicationEventPublisher publisher = mock(org.springframework.context.ApplicationEventPublisher.class);
        orchestrator = new AnalyticsOrchestrator(
                sectionEngine, rankingEngine, new ReportAssembler(), persistence, mapper, publisher);
        when(persistence.claim(event)).thenReturn(true);
        when(sectionEngine.analyze(event)).thenReturn(sectionResult);
        when(rankingEngine.processRankAndStats(event)).thenReturn(peer);
    }

    @Test
    void invokesBothEnginesAssemblesReportAndPersists() {
        orchestrator.process(event);

        verify(sectionEngine).analyze(event);
        verify(rankingEngine).processRankAndStats(event);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(persistence).complete(eq(event), eq(sectionResult), eq(peer), json.capture());

        JsonNode node = mapper.readTree(json.getValue());
        assertThat(node.get("metadata").get("attemptId").textValue()).isEqualTo("att-1");
        assertThat(node.get("summary").get("rank").intValue()).isEqualTo(42);
        assertThat(node.get("summary").get("timeTakenSeconds").longValue()).isEqualTo(5420L);
        assertThat(node.get("sectionBreakdown").size()).isEqualTo(1);
        assertThat(node.get("peerComparison").get("topperScore").doubleValue()).isEqualTo(285.0);
        verify(persistence, never()).markFailed(any());
    }

    @Test
    void duplicateOfCompletedReportIsSkipped() {
        when(persistence.claim(event)).thenReturn(false);

        orchestrator.process(event);

        verify(sectionEngine, never()).analyze(any());
        verify(rankingEngine, never()).processRankAndStats(any());
        verify(persistence, never()).complete(any(), any(), any(), any());
    }

    @Test
    void concurrentClaimIsReEvaluatedFromExistingStatus() {
        when(persistence.claim(event))
                .thenThrow(new DataIntegrityViolationException("dup attempt_id"))
                .thenReturn(false);

        orchestrator.process(event);

        verify(persistence, times(2)).claim(event);
        verify(sectionEngine, never()).analyze(any());
        verify(persistence, never()).complete(any(), any(), any(), any());
    }

    @Test
    void sectionEngineFailureDoesNotComplete() {
        when(sectionEngine.analyze(event)).thenThrow(new RuntimeException("section boom"));

        assertThatThrownBy(() -> orchestrator.process(event))
                .isInstanceOf(AnalyticsProcessingException.class);

        verify(persistence, never()).complete(any(), any(), any(), any());
        verify(persistence).markFailed("att-1");
    }

    @Test
    void rankingEngineFailureDoesNotComplete() {
        when(rankingEngine.processRankAndStats(event)).thenThrow(new RuntimeException("rank boom"));

        assertThatThrownBy(() -> orchestrator.process(event))
                .isInstanceOf(AnalyticsProcessingException.class);

        verify(persistence, never()).complete(any(), any(), any(), any());
        verify(persistence).markFailed("att-1");
    }

    @Test
    void incompleteEngineResultFailsReport() {
        when(rankingEngine.processRankAndStats(event))
                .thenReturn(new PeerComparisonResult(1.0, 1.0, 1L, null, 1, 1.0));

        assertThatThrownBy(() -> orchestrator.process(event))
                .isInstanceOf(AnalyticsProcessingException.class);

        verify(persistence, never()).complete(any(), any(), any(), any());
        verify(persistence).markFailed("att-1");
    }

    @Test
    void invalidEventIsRejectedBeforeClaiming() {
        AttemptSubmittedEvent legacyLike = new AttemptSubmittedEvent("event-2", "att-2", "u", null, null, "t", null, null,
                Instant.parse("2026-01-01T00:00:00Z"));

        assertThatThrownBy(() -> orchestrator.process(legacyLike))
                .isInstanceOf(InvalidAttemptEventException.class);

        verify(persistence, never()).claim(any());
        verify(sectionEngine, never()).analyze(any());
    }
}