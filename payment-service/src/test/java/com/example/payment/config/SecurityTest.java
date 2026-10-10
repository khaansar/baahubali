package com.example.payment.config;

import jakarta.servlet.FilterChain;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SecurityTest {

    private InternalAuthFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        InternalAuthProperties properties = new InternalAuthProperties();
        properties.setMySecret("payment-secret");
        properties.setAllowedClients(Map.of(
                "api-gateway", "gateway-secret",
                "attempt-service", "attempt-secret",
                "test-service", "test-secret"));

        filter = new InternalAuthFilter(properties);
        chain = mock(FilterChain.class);
    }

    private MockHttpServletResponse run(
            MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    private MockHttpServletRequest request(
            String path, String caller, String secret) {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", path);
        if (caller != null) {
            request.addHeader("X-Service-Caller", caller);
        }
        if (secret != null) {
            request.addHeader("X-Service-Auth", secret);
        }
        return request;
    }

    @Test
    void rejectsMissingServiceHeaders() throws Exception {
        assertEquals(401, run(request("/internal/orders/1", null, null))
                .getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void rejectsOldInternalTokenHeader() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/internal/orders/1");
        request.addHeader("X-Internal-Token", "attempt-secret");

        assertEquals(401, run(request).getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void rejectsCorrectSecretForWrongCaller() throws Exception {
        assertEquals(401, run(request(
                "/internal/orders/1",
                "test-service",
                "attempt-secret")).getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void acceptsConfiguredCallerAndSecret() throws Exception {
        assertEquals(200, run(request(
                "/internal/entitlements/check",
                "attempt-service",
                "attempt-secret")).getStatus());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void requiresServiceAuthOnNonInternalApiPathsToo() throws Exception {
        assertEquals(401, run(request("/orders", null, null)).getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void actuatorHealthProbeIsExempt() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/actuator/health");
        request.setServletPath("/actuator/health");

        assertEquals(200, run(request).getStatus());
        verify(chain).doFilter(any(), any());
    }
}