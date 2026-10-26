package com.example.analyticsservice.core.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.analyticsservice.client.AttemptServiceClient;
import com.example.analyticsservice.contract.AttemptSubmittedEvent;
import com.example.analyticsservice.contract.PeerComparisonResult;
import com.example.analyticsservice.contract.RankingEngine;
import com.example.analyticsservice.contract.SectionAnalysisEngine;
import com.example.analyticsservice.contract.SectionAnalysisResult;
import com.example.analyticsservice.web.event.ReportReadyEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.json.JsonMapper;

class AnalyticsOrchestratorTest {

    @Test
    void storesCalculatedScoreOnAttemptBeforeCompletingReport() {
        SectionAnalysisEngine sectionAnalysisEngine = mock(SectionAnalysisEngine.class);
        RankingEngine rankingEngine = mock(RankingEngine.class);
        ReportPersistenceService persistence = mock(ReportPersistenceService.class);
        AttemptServiceClient attemptServiceClient = mock(AttemptServiceClient.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        AnalyticsOrchestrator orchestrator = new AnalyticsOrchestrator(
                sectionAnalysisEngine,
                rankingEngine,
                new ReportAssembler(),
                persistence,
                attemptServiceClient,
                JsonMapper.builder().build(),
                eventPublisher);

        AttemptSubmittedEvent event = new AttemptSubmittedEvent(
                "event-1", "attempt-1", "user-1", "category-1", "series-1", "test-1",
                60L, List.of(), Instant.parse("2026-01-01T00:00:00Z"));
        SectionAnalysisResult score = new SectionAnalysisResult(List.of(), 42.5, 100.0, 42.5);
        PeerComparisonResult peer = new PeerComparisonResult(42.5, 42.5, 0L, 1, 1, 100.0);

        when(persistence.claim(event)).thenReturn(true);
        when(sectionAnalysisEngine.analyze(event)).thenReturn(score);
        when(rankingEngine.processRankAndStats(event)).thenReturn(peer);

        orchestrator.process(event);

        verify(attemptServiceClient).updateFinalScore("attempt-1", 42.5);
        verify(persistence).complete(eq(event), eq(score), eq(peer), org.mockito.ArgumentMatchers.anyString());
        verify(eventPublisher).publishEvent(new ReportReadyEvent("attempt-1"));
    }
}
