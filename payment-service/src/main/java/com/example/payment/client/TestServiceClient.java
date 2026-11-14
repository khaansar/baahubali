package com.example.payment.client;

import com.example.payment.config.PaymentProperties;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class TestServiceClient {

    private final RestClient http;

    public TestServiceClient(
            PaymentProperties properties,
            @Value("${internal.auth.my-secret}") String secret,
            RestClient.Builder builder) {

        this.http = builder
                .baseUrl(properties.getTestServiceUrl())
                .defaultHeader("X-Service-Caller", "payment-service")
                .defaultHeader("X-Service-Auth", secret)
                .build();
    }

    public UUID seriesIdOf(UUID testId) {
        try {
            SeriesRef response = http.get()
                    .uri("/tests-api/internal/tests/{id}/series", testId)
                    .retrieve()
                    .body(SeriesRef.class);

            String seriesId = response == null ? null : response.seriesId();

            return seriesId == null || seriesId.isBlank()
                    ? null
                    : UUID.fromString(seriesId);

        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (RestClientException | HttpMessageConversionException e) {
            throw new PaymentException(
                    ErrorCode.UPSTREAM_UNAVAILABLE,
                    "Test service unavailable");
        }
    }

    public String seriesSlugOf(UUID seriesId) {
        if (seriesId == null) {
            return null;
        }

        try {
            SeriesSlug response = http.get()
                    .uri("/tests-api/internal/series/{seriesId}", seriesId)
                    .retrieve()
                    .body(SeriesSlug.class);

            String slug = response == null ? null : response.slug();

            return slug == null || slug.isBlank()
                    ? null
                    : slug;

        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (RestClientException | HttpMessageConversionException e) {
            throw new PaymentException(
                    ErrorCode.UPSTREAM_UNAVAILABLE,
                    "Test service unavailable while resolving test-series slug"
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeriesRef(String seriesId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeriesSlug(String slug) {
    }
}
