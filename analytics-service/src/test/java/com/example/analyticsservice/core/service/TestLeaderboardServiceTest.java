package com.example.analyticsservice.core.service;

import com.example.analyticsservice.client.IamServiceClient;
import com.example.analyticsservice.client.ServiceResponse;
import com.example.analyticsservice.client.TestServiceClient;
import com.example.analyticsservice.contract.TestLeaderboardResponse;
import com.example.analyticsservice.contract.TestLeaderboardRow;
import com.example.analyticsservice.core.repository.TestReportRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestLeaderboardServiceTest {

    private TestReportRepository reports;
    private TestServiceClient testServiceClient;
    private IamServiceClient iamServiceClient;
    private TestLeaderboardService service;
    private UUID testId;

    @BeforeEach
    void setUp() {
        reports = mock(TestReportRepository.class);
        testServiceClient = mock(TestServiceClient.class);
        iamServiceClient = mock(IamServiceClient.class);
        service = new TestLeaderboardService(reports, testServiceClient, iamServiceClient);
        testId = UUID.fromString("9cfc57bf-06a6-4e5c-93f0-705c708a1a2e");

        when(testServiceClient.getPublishedTestStatusBySlug("physics-mock-1"))
                .thenReturn(new ServiceResponse<>(true, 200, "ok",
                        new TestServiceClient.PublishedTestStatus(testId)));
    }

    @Test
    void resolvesTheSlugThenReturnsHydratedUserProfiles() {
        TestLeaderboardRow row = new TestLeaderboardRow(
                "user-1", new BigDecimal("95.456"), new BigDecimal("100.004"),
                new BigDecimal("66.666"), 420, LocalDateTime.parse("2026-10-01T10:00:00"));
        when(reports.findFirstAttemptLeaderboard(testId.toString(), PageRequest.of(0, 25)))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 25), 1));
        when(iamServiceClient.getUsersBatch(List.of("user-1")))
                .thenReturn(new ServiceResponse<>(true, 200, "ok", Map.of(
                        "user-1", new IamServiceClient.UserProfile(
                                "user-1", "Ada Lovelace", "https://cdn.example/ada.png")
                )));

        TestLeaderboardResponse response = service.getLeaderboard("  physics-mock-1  ", 0, 25);

        assertThat(response.testSlug()).isEqualTo("physics-mock-1");
        assertThat(response.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.rank()).isEqualTo(1);
            assertThat(entry.userName()).isEqualTo("Ada Lovelace");
            assertThat(entry.avatarUrl()).isEqualTo("https://cdn.example/ada.png");
            assertThat(entry.score()).isEqualByComparingTo("95.46");
            assertThat(entry.maxScore()).isEqualByComparingTo("100.00");
            assertThat(entry.accuracy()).isEqualByComparingTo("66.67");
        });
        verify(testServiceClient).getPublishedTestStatusBySlug("physics-mock-1");
    }

    @Test
    void usesAnonymousNameWhenIamHasNoProfileForALeaderboardUser() {
        TestLeaderboardRow row = new TestLeaderboardRow(
                "deleted-user", BigDecimal.TEN, BigDecimal.TEN,
                BigDecimal.valueOf(100), 120, LocalDateTime.parse("2026-10-01T10:00:00"));
        when(reports.findFirstAttemptLeaderboard(any(), any()))
                .thenReturn(new PageImpl<>(List.of(row)));
        when(iamServiceClient.getUsersBatch(List.of("deleted-user")))
                .thenReturn(new ServiceResponse<>(true, 200, "ok", Map.of()));

        TestLeaderboardResponse response = service.getLeaderboard("physics-mock-1", 0, 25);

        assertThat(response.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.userName()).isEqualTo("Anonymous User");
            assertThat(entry.avatarUrl()).isNull();
        });
    }
}
