package com.example.payment.client;

import com.example.payment.config.PaymentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/*
 * TEST_SERVICE_URL already ends with test-service's /tests-api context path,
 * as it does for every other service, so the client must not repeat it.
 */
class TestServiceClientTest {

    private static final String BASE_URL = "http://test-service:8080/tests-api";

    private MockRestServiceServer server;
    private TestServiceClient client;

    @BeforeEach
    void setUp() {
        PaymentProperties properties = new PaymentProperties();
        properties.setTestServiceUrl(BASE_URL);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TestServiceClient(properties, "secret", builder);
    }

    @Test
    void resolvesTheSeriesOfATestUnderTheConfiguredBaseUrl() {
        UUID testId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/tests/" + testId + "/series"))
                .andRespond(withSuccess("{\"testId\":\"" + testId + "\",\"seriesId\":\"" + seriesId + "\"}",
                        MediaType.APPLICATION_JSON));

        assertEquals(seriesId, client.seriesIdOf(testId));
        server.verify();
    }

    @Test
    void resolvesTheSlugOfASeriesUnderTheConfiguredBaseUrl() {
        UUID seriesId = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/series/" + seriesId))
                .andRespond(withSuccess("{\"seriesId\":\"" + seriesId + "\",\"slug\":\"neet\"}",
                        MediaType.APPLICATION_JSON));

        assertEquals("neet", client.seriesSlugOf(seriesId));
        server.verify();
    }
}
