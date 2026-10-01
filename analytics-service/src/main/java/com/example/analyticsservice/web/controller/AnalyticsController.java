package com.example.analyticsservice.web.controller;

import com.example.analyticsservice.contract.ReportData;
import com.example.analyticsservice.contract.UserPerformanceResponse;
import com.example.analyticsservice.web.service.ReportCacheService;
import com.example.analyticsservice.web.service.ReportNotificationService;
import com.example.analyticsservice.core.service.UserPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ReportCacheService reportCacheService;
    private final ReportNotificationService reportNotificationService;
    private final UserPerformanceService userPerformanceService;

    @GetMapping(value = "/performance", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserPerformanceResponse> getUserPerformance(
            @RequestHeader("X-User-Id") String userId) {

        UserPerformanceResponse performance =
                userPerformanceService.getPerformance(userId);

        return ResponseEntity.ok(performance);
    }

    @GetMapping(value = "/{attemptId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReportData> getReport(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String attemptId) {

        ReportData reportData =
                reportCacheService.getReportData(attemptId, userId);

        return ResponseEntity.ok(reportData);
    }

    @GetMapping(
            value = "/{attemptId}/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamReportStatus(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String attemptId) {

        SseEmitter emitter =
                reportNotificationService.subscribe(attemptId);

        try {
            reportCacheService.getReportData(attemptId, userId);

            try {
                emitter.send(
                        SseEmitter.event()
                                .data("{\"type\":\"REPORT_READY\",\"attemptId\":\""
                                        + attemptId
                                        + "\"}")
                );
                emitter.complete();
            } catch (java.io.IOException e) {
                emitter.completeWithError(e);
            }
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            if (ex.getStatusCode().value() != 202) {
                emitter.completeWithError(ex);
                throw ex;
            }
        } catch (Exception ex) {
            emitter.completeWithError(ex);
            throw ex;
        }

        return emitter;
    }
}