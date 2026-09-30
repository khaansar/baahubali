package com.example.analyticsservice.core.service;

import com.example.analyticsservice.core.exception.*;

import com.example.analyticsservice.core.entity.*;
import com.example.analyticsservice.core.repository.*;

import com.example.analyticsservice.contract.AttemptSubmittedEvent;
import com.example.analyticsservice.contract.PeerComparisonResult;
import com.example.analyticsservice.contract.RankingEngine;
import com.example.analyticsservice.contract.ReportData;
import com.example.analyticsservice.contract.SectionAnalysisEngine;
import com.example.analyticsservice.contract.SectionAnalysisResult;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import com.example.analyticsservice.web.event.ReportReadyEvent;
import tools.jackson.databind.json.JsonMapper;

/**
 * Coordinates the pipeline. Deliberately NOT @Transactional: engines run outside any
 * database transaction; persistence happens in short transactions via ReportPersistenceService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsOrchestrator {

    private final SectionAnalysisEngine sectionAnalysisEngine;
    private final RankingEngine rankingEngine;
    private final ReportAssembler reportAssembler;
    private final ReportPersistenceService persistence;
    private final JsonMapper jsonMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void process(AttemptSubmittedEvent event) {
        validate(event);
        String attemptId = event.attemptId();

        if (!claimWithRecheck(event)) {
            return; // already COMPLETED: duplicate delivery
        }

        try {
            SectionAnalysisResult sectionResult = sectionAnalysisEngine.analyze(event);
            PeerComparisonResult peer = rankingEngine.processRankAndStats(event);
            verifyEngineResults(sectionResult, peer);

            ReportData report = reportAssembler.assemble(event, sectionResult, peer);
            String reportJson = jsonMapper.writeValueAsString(report);

            persistence.complete(event, sectionResult, peer, reportJson);
            log.info("Analytics report completed, attemptId={}, testId={}", attemptId, event.testId());
            applicationEventPublisher.publishEvent(new ReportReadyEvent(attemptId));
        } catch (RuntimeException ex) {
            log.error("Analytics processing failed, attemptId={}, testId={}", attemptId, event.testId(), ex);
            try {
                persistence.markFailed(attemptId);
            } catch (RuntimeException markEx) {
                log.error("Could not mark report FAILED, attemptId={}", attemptId, markEx);
                ex.addSuppressed(markEx);
            }
            throw new AnalyticsProcessingException("Analytics processing failed for attemptId=" + attemptId, ex);
        }
    }

    /**
     * If a concurrent delivery inserted the same attempt first, the unique constraint fires.
     * We do not ignore that: we re-read the row and decide from its status
     * (COMPLETED -> skip, otherwise process).
     */
    private boolean claimWithRecheck(AttemptSubmittedEvent event) {
        try {
            return persistence.claim(event);
        } catch (DataIntegrityViolationException ex) {
            log.info("Concurrent claim detected, re-evaluating existing report, attemptId={}", event.attemptId());
            return persistence.claim(event);
        }
    }

    private static void validate(AttemptSubmittedEvent event) {
        if (event == null) {
            throw new InvalidAttemptEventException("Event is null");
        }
        List<String> missing = new ArrayList<>();
        if (isBlank(event.attemptId())) missing.add("attemptId");
        if (isBlank(event.userId())) missing.add("userId");
        if (isBlank(event.categoryId())) missing.add("categoryId");
        if (isBlank(event.testSeriesId())) missing.add("testSeriesId");
        if (isBlank(event.testId())) missing.add("testId");
        if (event.timeTakenSeconds() == null) missing.add("timeTakenSeconds");
        if (event.sectionAnswers() == null) missing.add("sectionAnswers");
        if (!missing.isEmpty()) {
            throw new InvalidAttemptEventException(
                    "AttemptSubmittedEvent missing required fields " + missing + " (attemptId=" + event.attemptId() + ")");
        }
    }

    private static void verifyEngineResults(SectionAnalysisResult sectionResult, PeerComparisonResult peer) {
        if (sectionResult == null || sectionResult.sections() == null
                || sectionResult.totalScore() == null || sectionResult.maxScore() == null
                || sectionResult.accuracyPercentage() == null) {
            throw new IllegalStateException("SectionAnalysisEngine returned an incomplete result");
        }
        if (peer == null || peer.rank() == null) {
            throw new IllegalStateException("RankingEngine returned an incomplete result (rank required)");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}