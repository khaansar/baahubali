package com.example.payment.config;

import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SecurityTest {
    InternalAuthFilter filter; CurrentUserResolver me; FilterChain chain;

    @BeforeEach void setUp() {
        PaymentProperties p = new PaymentProperties(); p.setInternalToken("secret"); p.setAdminRoles("ADMIN,SUPER_ADMIN"); p.setRefundRoles("SUPER_ADMIN");
        filter = new InternalAuthFilter(p); me = new CurrentUserResolver(p); chain = mock(FilterChain.class);
    }
    MockHttpServletResponse run(MockHttpServletRequest req) throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse(); filter.doFilter(req, res, chain); return res;
    }

    @Test void internalEndpointWithoutTokenRejected() throws Exception {
        assertEquals(401, run(new MockHttpServletRequest("POST", "/internal/entitlements/check")).getStatus());
        verify(chain, never()).doFilter(any(), any());
    }
    @Test void internalEndpointWithUserIdHeaderAloneRejected() throws Exception {
        var req = new MockHttpServletRequest("POST", "/internal/entitlements/check"); req.addHeader("X-User-Id", UUID.randomUUID().toString());
        assertEquals(401, run(req).getStatus());
    }
    @Test void internalEndpointWithWrongTokenRejected() throws Exception {
        var req = new MockHttpServletRequest("POST", "/internal/orders/1"); req.addHeader("X-Internal-Token", "wrong");
        assertEquals(401, run(req).getStatus());
    }
    @Test void internalEndpointWithCorrectTokenPasses() throws Exception {
        var req = new MockHttpServletRequest("POST", "/internal/orders/1"); req.addHeader("X-Internal-Token", "secret");
        assertEquals(200, run(req).getStatus());
        verify(chain).doFilter(any(), any());
    }
    @Test void publicPathNotSubjectToInternalFilter() throws Exception {
        run(new MockHttpServletRequest("GET", "/payments-api/orders"));
        verify(chain).doFilter(any(), any());
    }
    @Test void missingUserHeaderIsUnauthenticated() {
        var ex = assertThrows(PaymentException.class, () -> me.userId(new MockHttpServletRequest()));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }
    @Test void malformedUserHeaderIsUnauthenticated() {
        var req = new MockHttpServletRequest(); req.addHeader("X-User-Id", "not-a-uuid");
        assertThrows(PaymentException.class, () -> me.userId(req));
    }
    @Test void nonAdminForbidden() {
        var req = new MockHttpServletRequest(); req.addHeader("X-User-Id", UUID.randomUUID().toString()); req.addHeader("X-User-Role", "STUDENT");
        assertEquals(ErrorCode.FORBIDDEN, assertThrows(PaymentException.class, () -> me.requireAdmin(req)).getCode());
    }
    @Test void adminButNotRefundRoleForbiddenForRefunds() {
        var req = new MockHttpServletRequest(); req.addHeader("X-User-Id", UUID.randomUUID().toString()); req.addHeader("X-User-Role", "ADMIN");
        me.requireAdmin(req);
        assertThrows(PaymentException.class, () -> me.requireRefundAdmin(req));
    }
}