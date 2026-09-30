package com.example.analyticsservice.web.service;

import com.example.analyticsservice.contract.ReportData;
import com.example.analyticsservice.core.entity.ReportStatus;
import com.example.analyticsservice.core.entity.TestReportEntity;
import com.example.analyticsservice.core.repository.TestReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportCacheService {

    private final StringRedisTemplate redisTemplate;
    private final TestReportRepository testReportRepository;
    private final JsonMapper jsonMapper;

    private static final String CACHE_KEY_PREFIX = "analytics:report:";
    private static final long CACHE_TTL_DAYS = 30;

    public ReportData getReportData(String attemptId, String userId) {
        String cacheKey = CACHE_KEY_PREFIX + attemptId;

        Object cachedUserId =
                redisTemplate.opsForHash().get(cacheKey, "userId");

        if (cachedUserId != null) {
            if (!cachedUserId.toString().equals(userId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Access denied"
                );
            }

            Object cachedData =
                    redisTemplate.opsForHash().get(cacheKey, "data");

            if (cachedData != null) {
                try {
                    return jsonMapper.readValue(
                            cachedData.toString(),
                            ReportData.class
                    );
                } catch (Exception e) {
                    log.warn(
                            "Failed to parse cached report data for attemptId={}",
                            attemptId,
                            e
                    );
                }
            }
        }

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

        redisTemplate.opsForHash().put(
                cacheKey,
                "userId",
                entity.getUserId()
        );

        redisTemplate.opsForHash().put(
                cacheKey,
                "data",
                entity.getReportData()
        );

        redisTemplate.expire(
                cacheKey,
                CACHE_TTL_DAYS,
                TimeUnit.DAYS
        );

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
}