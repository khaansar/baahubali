package com.example.attemptservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Retrieves the completed score owned by analytics-service. */
@org.springframework.stereotype.Component
public class AnalyticsServiceClient {

    private final RestClient http;

    public AnalyticsServiceClient(
            @Value("${internal.services.analytics-url}") String url,
            @Value("${internal.auth.my-secret}") String secret,
            RestClient.Builder builder) {
        this.http = builder
                .baseUrl(url)
                .defaultHeader("X-Service-Caller", "attempt-service")
                .defaultHeader("X-Service-Auth", secret)
                .build();
    }

    /**
     * Returns null until analytics has completed the report (or when it has
     * failed to produce one). Transport and authorization failures remain
     * visible to callers instead of silently presenting an incorrect score.
     */
    public Double getTotalScore(String attemptId, String userId) {
        try {
            AnalyticsReportResponse response = http.get()
                    .uri("/reports/{attemptId}", attemptId)
                    .header("X-User-Id", userId)
                    .retrieve()
                    .body(AnalyticsReportResponse.class);

            if (response == null || !response.success() || response.data() == null
                    || response.data().summary() == null) {
                return null;
            }
            return response.data().summary().totalScore();
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == HttpStatus.NOT_FOUND.value()
                    || status == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
                return null;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Analytics service could not complete the request", ex);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Analytics service could not complete the request", ex);
        }
    }

    public record AnalyticsReportResponse(boolean success, AnalyticsReportData data) {
    }

    public record AnalyticsReportData(AnalyticsReportSummary summary) {
    }

    public record AnalyticsReportSummary(Double totalScore) {
    }
}
