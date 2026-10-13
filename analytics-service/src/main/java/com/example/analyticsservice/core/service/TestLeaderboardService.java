package com.example.analyticsservice.core.service;

import com.example.analyticsservice.client.IamServiceClient;
import com.example.analyticsservice.client.ServiceResponse;
import com.example.analyticsservice.client.TestServiceClient;
import com.example.analyticsservice.contract.TestLeaderboardEntry;
import com.example.analyticsservice.contract.TestLeaderboardResponse;
import com.example.analyticsservice.contract.TestLeaderboardRow;
import com.example.analyticsservice.core.repository.TestReportRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import feign.FeignException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TestLeaderboardService {

    private final TestReportRepository testReportRepository;
    private final TestServiceClient testServiceClient;
    private final IamServiceClient iamServiceClient;

    @Transactional(readOnly = true)
    public TestLeaderboardResponse getLeaderboard(String testSlug, int page, int pageSize) {
        String normalizedTestSlug = normalizeTestSlug(testSlug);
        String testId = resolvePublishedTestId(normalizedTestSlug);
        int normalizedPage = Math.max(0, page);
        int normalizedPageSize = Math.max(1, Math.min(pageSize, 100));
        Pageable pageable = PageRequest.of(normalizedPage, normalizedPageSize);
        Page<TestLeaderboardRow> results = testReportRepository.findFirstAttemptLeaderboard(testId, pageable);
        List<TestLeaderboardEntry> entries = new ArrayList<>(results.getContent().size());
        Map<String, IamServiceClient.UserProfile> profiles = getUserProfiles(results.getContent());

        for (int i = 0; i < results.getContent().size(); i++) {
            TestLeaderboardRow row = results.getContent().get(i);
            IamServiceClient.UserProfile profile = profiles.get(row.userId());
            entries.add(new TestLeaderboardEntry(
                    normalizedPage * normalizedPageSize + i + 1,
                    profileName(profile),
                    profile != null ? profile.avatarUrl() : null,
                    row.score(),
                    row.maxScore(),
                    row.accuracyPercentage(),
                    row.timeTakenSeconds(),
                    row.firstAttemptAt()
            ));
        }

        return new TestLeaderboardResponse(
                normalizedTestSlug,
                normalizedPage + 1,
                normalizedPageSize,
                results.getTotalElements(),
                entries
        );
    }

    private String resolvePublishedTestId(String testSlug) {
        try {
            ServiceResponse<TestServiceClient.PublishedTestStatus> response =
                    testServiceClient.getPublishedTestStatusBySlug(testSlug);
            if (response == null || !response.success() || response.data() == null
                    || response.data().testId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Test service returned an invalid test lookup response"
                );
            }
            return response.data().testId().toString();
        } catch (FeignException.NotFound exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Test is not available in the public catalog"
            );
        } catch (FeignException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Test service is unavailable"
            );
        }
    }

    private Map<String, IamServiceClient.UserProfile> getUserProfiles(
            List<TestLeaderboardRow> rows) {
        Set<String> userIds = new LinkedHashSet<>();
        rows.forEach(row -> userIds.add(row.userId()));

        if (userIds.isEmpty()) {
            return Map.of();
        }

        try {
            ServiceResponse<Map<String, IamServiceClient.UserProfile>> response =
                    iamServiceClient.getUsersBatch(List.copyOf(userIds));
            if (response == null || !response.success() || response.data() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "IAM service returned an invalid user profile response"
                );
            }
            return response.data();
        } catch (FeignException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "IAM service is unavailable"
            );
        }
    }

    private static String normalizeTestSlug(String testSlug) {
        if (!StringUtils.hasText(testSlug)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test slug cannot be blank");
        }
        return testSlug.trim();
    }

    private static String profileName(IamServiceClient.UserProfile profile) {
        return profile != null && StringUtils.hasText(profile.displayName())
                ? profile.displayName().trim()
                : "Anonymous User";
    }
}
