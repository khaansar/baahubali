package com.example.analyticsservice.web.service;

import org.springframework.cache.annotation.CacheEvict;
import com.example.analyticsservice.contract.ReportData;
import com.example.analyticsservice.core.entity.ReportStatus;
import com.example.analyticsservice.core.entity.TestReportEntity;
import com.example.analyticsservice.core.repository.TestReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportCacheService {

    private final TestReportRepository testReportRepository;
    private final JsonMapper jsonMapper;

    private static final String REPORT_CACHE_NAME = "analytics:report";

    @Cacheable(
            cacheNames = REPORT_CACHE_NAME,
            key = "#attemptId + ':' + #userId",
            unless = "#result == null"
    )
    public ReportData getReportData(String attemptId, String userId) {
        TestReportEntity entity = testReportRepository.findByAttemptId(attemptId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Report not found"
                ));

        if (!entity.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied"
            );
        }

        if (ReportStatus.PROCESSING.name().equals(entity.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.ACCEPTED,
                    "Report is still processing"
            );
        }

        if (ReportStatus.FAILED.name().equals(entity.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Report generation failed"
            );
        }

        if (entity.getReportData() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Report data is empty"
            );
        }

        try {
            return jsonMapper.readValue(
                    entity.getReportData(),
                    ReportData.class
            );
        } catch (Exception e) {
            log.error(
                    "Failed to parse report data from DB for attemptId={}",
                    attemptId,
                    e
            );

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid report data format"
            );
        }
    }

    @CacheEvict(
            cacheNames = "analytics:report",
            key = "#attemptId + ':' + #userId"
    )
    public void evictReportData(String attemptId, String userId) {
        log.debug(
                "Evicted report cache for attemptId={}",
                attemptId
        );
    }
}