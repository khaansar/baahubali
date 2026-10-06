package com.example.analyticsservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class InternalAuthInterceptorTest {

    private InternalAuthInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new InternalAuthInterceptor(JsonMapper.builder().build());
        interceptor.getAllowedClients().put("attempt-service", "attempt-secret-key");
        request = new MockHttpServletRequest("GET", "/analytics-api/internal/ping");
        response = new MockHttpServletResponse();
    }

    @Test
    void missingCallerIsRejected() throws Exception {
        request.addHeader("X-Service-Auth", "attempt-secret-key");
        assertRejected();
    }

    @Test
    void missingSecretIsRejected() throws Exception {
        request.addHeader("X-Service-Caller", "attempt-service");
        assertRejected();
    }

    @Test
    void unknownCallerIsRejected() throws Exception {
        request.addHeader("X-Service-Caller", "unknown-service");
        request.addHeader("X-Service-Auth", "attempt-secret-key");
        assertRejected();
    }

    @Test
    void wrongSecretIsRejected() throws Exception {
        request.addHeader("X-Service-Caller", "attempt-service");
        request.addHeader("X-Service-Auth", "wrong");
        assertRejected();
    }

    @Test
    void validCredentialsAreAllowed() throws Exception {
        request.addHeader("X-Service-Caller", "attempt-service");
        request.addHeader("X-Service-Auth", "attempt-secret-key");
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private void assertRejected() throws Exception {
        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString())
                .contains("\"success\":false", "\"status\":401", "\"code\":\"INTERNAL_AUTH_FAILED\"",
                        "\"trace_id\":\"err-");
    }
}
