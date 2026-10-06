package com.example.analyticsservice.web.controller;

import com.example.analyticsservice.contract.ReportData;
import com.example.analyticsservice.contract.UserPerformanceResponse;
import com.example.analyticsservice.contract.UserTopicPerformanceResponse;
import com.example.analyticsservice.web.service.ReportCacheService;
import com.example.analyticsservice.web.service.ReportNotificationService;
import com.example.analyticsservice.web.dto.ApiResponse;
import com.example.analyticsservice.core.service.UserPerformanceService;
import org.springframework.beans.factory.annotation.Value;
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
public class AnalyticsController {

    private final ReportCacheService reportCacheService;
    private final ReportNotificationService reportNotificationService;
    private final UserPerformanceService userPerformanceService;
    private final String apiVersion;

    public AnalyticsController(ReportCacheService reportCacheService,
                               ReportNotificationService reportNotificationService,
                               UserPerformanceService userPerformanceService,
                               @Value("${app.api.version:1.2.0}") String apiVersion) {
        this.reportCacheService = reportCacheService;
        this.reportNotificationService = reportNotificationService;
        this.userPerformanceService = userPerformanceService;
        this.apiVersion = apiVersion;
    }

    @GetMapping(value = "/performance", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UserPerformanceResponse>> getUserPerformance(
            @RequestHeader("X-User-Id") String userId) {

        UserPerformanceResponse performance =
                userPerformanceService.getPerformance(userId);

        return ResponseEntity.ok(ApiResponse.success(200,
                "Performance data retrieved successfully", performance, apiVersion));
    }

    @GetMapping(value = "/topics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UserTopicPerformanceResponse>> getUserTopicPerformance(
            @RequestHeader("X-User-Id") String userId) {

        UserTopicPerformanceResponse topicPerformance =
                userPerformanceService.getTopicPerformance(userId);

        return ResponseEntity.ok(ApiResponse.success(200,
                "Topic performance retrieved successfully", topicPerformance, apiVersion));
    }

    @GetMapping(value = "/{attemptId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ReportData>> getReport(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String attemptId) {

        ReportData reportData =
                reportCacheService.getReportData(attemptId, userId);

        return ResponseEntity.ok(ApiResponse.success(200,
                "Report retrieved successfully", reportData, apiVersion));
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
                                .name("REPORT_READY")
                                .data(reportNotificationService.serializeReadyResponse(attemptId, apiVersion))
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
