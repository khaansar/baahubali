package com.example.analyticsservice.web.service;

import com.example.analyticsservice.contract.PeerComparisonResult;
import com.example.analyticsservice.contract.ReportData;
import com.example.analyticsservice.contract.ReportMetadata;
import com.example.analyticsservice.contract.ReportSummary;
import com.example.analyticsservice.contract.SectionReport;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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

    /**
     * Returns the report associated with an attempt after verifying ownership.
     *
     * All decimal-valued report fields are normalized to two decimal places
     * before the response is returned, regardless of whether the report was
     * loaded from Redis or the database.
     *
     * The normalized JSON is also written to Redis so that subsequent reads
     * use the normalized representation.
     */
    public ReportData getReportData(String attemptId, String userId) {

        String cacheKey = CACHE_KEY_PREFIX + attemptId;

        /*
         * First, check whether Redis already contains a report for this
         * attempt. The ownership check must happen before returning any
         * cached report data.
         */
        Object cachedUserId = redisTemplate.opsForHash().get(cacheKey, "userId");

        if (cachedUserId != null) {

            if (!cachedUserId.toString().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }

            Object cachedData = redisTemplate.opsForHash().get(cacheKey, "data");

            if (cachedData != null) {
                try {
                    ReportData cachedReport = jsonMapper.readValue(cachedData.toString(), ReportData.class);

                    /*
                     * Normalize cached reports as well. Redis may contain
                     * JSON written before the rounding change was deployed.
                     */
                    ReportData normalizedReport = normalizeReportData(cachedReport);

                    /*
                     * Refresh the cached representation with normalized
                     * values. If serialization fails, the normalized report
                     * can still be returned to the current caller.
                     */
                    cacheNormalizedReport(cacheKey, userId, normalizedReport);

                    return normalizedReport;

                } catch (Exception e) {
                    log.warn("Failed to parse cached report data for attemptId={}", attemptId, e);

                    /*
                     * Fall through to the database lookup. This preserves
                     * the existing fallback behavior when cached JSON is
                     * invalid or cannot be processed.
                     */
                }
            }
        }

        /*
         * Redis did not provide usable report data. Load the persisted
         * report and verify ownership before exposing its contents.
         */
        TestReportEntity entity = testReportRepository.findByAttemptId(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        if (!entity.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        /*
         * Preserve the existing report lifecycle checks.
         */
        if (ReportStatus.PROCESSING.name().equals(entity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.ACCEPTED, "Report is still processing");
        }

        if (ReportStatus.FAILED.name().equals(entity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Report generation failed");
        }

        if (entity.getReportData() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report data is empty");
        }

        try {
            /*
             * Deserialize the persisted report JSON into the existing
             * response contract.
             */
            ReportData reportData = jsonMapper.readValue(entity.getReportData(), ReportData.class);

            /*
             * Normalize every decimal-valued field before the report is
             * cached or returned to the API controller.
             */
            ReportData normalizedReport = normalizeReportData(reportData);

            /*
             * Cache the normalized JSON, not the original unrounded JSON.
             */
            cacheNormalizedReport(cacheKey, entity.getUserId(), normalizedReport);

            return normalizedReport;

        } catch (ResponseStatusException e) {
            throw e;

        } catch (Exception e) {
            log.error("Failed to parse or normalize report data from DB for attemptId={}", attemptId, e);

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid report data format");
        }
    }

    /**
     * Reconstructs the report response with every decimal-valued field
     * rounded to two decimal places.
     *
     * Integer-valued fields, identifiers, metadata, and time values remain
     * unchanged.
     */
    private ReportData normalizeReportData(ReportData reportData) {

        if (reportData == null) {
            return null;
        }

        ReportMetadata metadata = reportData.metadata();

        ReportSummary summary = normalizeSummary(reportData.summary());

        List<SectionReport> sectionBreakdown = normalizeSectionBreakdown(reportData.sectionBreakdown());

        PeerComparisonResult peerComparison = normalizePeerComparison(reportData.peerComparison());

        return new ReportData(
                metadata,
                summary,
                sectionBreakdown,
                peerComparison
        );
    }

    /**
     * Rounds the decimal fields in the overall report summary.
     *
     * Rank, participant count, and time taken are integer-valued fields
     * and are intentionally not modified.
     */
    private ReportSummary normalizeSummary(ReportSummary summary) {

        if (summary == null) {
            return null;
        }

        return new ReportSummary(
                round(summary.totalScore()),
                round(summary.maxScore()),
                summary.rank(),
                summary.totalParticipants(),
                round(summary.percentile()),
                round(summary.accuracy()),
                summary.timeTakenSeconds()
        );
    }

    /**
     * Rounds each section's score, maximum score, and accuracy.
     *
     * Question counts and time values retain their original values.
     */
    private List<SectionReport> normalizeSectionBreakdown(List<SectionReport> sectionBreakdown) {

        if (sectionBreakdown == null) {
            return null;
        }

        return sectionBreakdown.stream()
                .map(section -> {
                    if (section == null) {
                        return null;
                    }

                    return new SectionReport(
                            section.sectionId(),
                            section.sectionName(),
                            round(section.score()),
                            round(section.maxScore()),
                            section.totalQuestions(),
                            section.correct(),
                            section.incorrect(),
                            section.unattempted(),
                            round(section.accuracy()),
                            section.timeSpentSeconds()
                    );
                })
                .toList();
    }

    /**
     * Rounds all decimal-valued peer comparison metrics.
     *
     * Rank, participant count, and time taken remain unchanged.
     */
    private PeerComparisonResult normalizePeerComparison(PeerComparisonResult peerComparison) {

        if (peerComparison == null) {
            return null;
        }

        return new PeerComparisonResult(
                round(peerComparison.topperScore()),
                round(peerComparison.averageScore()),
                peerComparison.topperTimeTakenSeconds(),
                peerComparison.rank(),
                peerComparison.totalParticipants(),
                round(peerComparison.percentile())
        );
    }

    /**
     * Null-safe decimal rounding using HALF_UP.
     *
     * Example:
     * 85.456 -> 85.46
     * 85.454 -> 85.45
     * 85.455 -> 85.46
     *
     * The report contracts currently use Double for these values, so this
     * method returns a rounded Double while preserving null values.
     */
    private Double round(Double value) {

        if (value == null) {
            return null;
        }

        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Writes normalized report data to Redis and refreshes the cache TTL.
     *
     * Cache-write failures should not prevent a valid report from being
     * returned to the caller.
     */
    private void cacheNormalizedReport(String cacheKey, String userId, ReportData normalizedReport) {

        try {
            String normalizedJson = jsonMapper.writeValueAsString(normalizedReport);

            redisTemplate.opsForHash().put(cacheKey, "userId", userId);

            redisTemplate.opsForHash().put(cacheKey, "data", normalizedJson);

            redisTemplate.expire(cacheKey, CACHE_TTL_DAYS, TimeUnit.DAYS);

        } catch (Exception e) {
            log.warn("Failed to cache normalized report data for key={}", cacheKey, e);
        }
    }

    
    /**
     * Evicts the cached report for an attempt after verifying that the
     * cached entry belongs to the supplied user.
     *
     * This method is called by ReportPersistenceService when a report is
     * persisted or updated, ensuring that subsequent reads can load the
     * latest report data instead of returning stale cached JSON.
     *
     * @param attemptId the attempt whose report cache should be evicted
     * @param userId    the user who owns the report
     */
    public void evictReportData(String attemptId, String userId) {

        if (attemptId == null || attemptId.isBlank()
                || userId == null || userId.isBlank()) {
            log.warn(
                    "Skipping report cache eviction because attemptId or userId is blank"
            );
            return;
        }

        String cacheKey = CACHE_KEY_PREFIX + attemptId;

        Object cachedUserId =
                redisTemplate.opsForHash().get(cacheKey, "userId");

        if (cachedUserId == null) {
            /*
             * There is no cached owner to verify. Avoid deleting a cache
             * entry without confirming ownership.
             */
            log.debug(
                    "No cached report owner found for attemptId={}",
                    attemptId
            );
            return;
        }

        if (!userId.equals(cachedUserId.toString())) {
            log.warn(
                    "Skipping report cache eviction because the cached owner does not match, attemptId={}",
                    attemptId
            );
            return;
        }

        Boolean deleted = redisTemplate.delete(cacheKey);

        log.debug(
                "Report cache eviction completed for attemptId={}, deleted={}",
                attemptId,
                Boolean.TRUE.equals(deleted)
        );
    }

}