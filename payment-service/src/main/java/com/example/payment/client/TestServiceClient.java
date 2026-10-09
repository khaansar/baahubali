package com.example.payment.client;

import com.example.payment.config.PaymentProperties;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.springframework.http.client.RestClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@Component
public class TestServiceClient {

    private final RestClient http;

    public TestServiceClient(PaymentProperties properties, RestClient.Builder builder) {
        this.http = builder.baseUrl(properties.getTestServiceUrl()).defaultHeader("X-Internal-Token", properties.getInternalToken()).build();
    }

    public UUID seriesIdOf(UUID testId) {
        try {
            var ref = http.get().uri("/internal/tests/{id}/series", testId).retrieve().body(JsonNode.class);

            String seriesId = ref.path("seriesId").asText(null);

            return seriesId == null ? null : UUID.fromString(seriesId);

        } catch (HttpClientErrorException.NotFound e) {
            return null;

        } catch (RestClientException e) {
            throw new PaymentException(ErrorCode.UPSTREAM_UNAVAILABLE, "Test service unavailable");
        }
    }
}