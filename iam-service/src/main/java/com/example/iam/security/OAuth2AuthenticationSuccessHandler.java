package com.example.iam.security;

import com.example.iam.dto.AuthenticationResult;
import com.example.iam.entity.User;
import com.example.iam.service.OAuth2UserProvisioningService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler
        implements AuthenticationSuccessHandler {

    public static final String NEXT_SESSION_ATTRIBUTE =
            "CLEARIT_OAUTH_NEXT";

    private final OAuth2UserProvisioningService provisioningService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieFactory authCookieFactory;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            response.sendRedirect("/login?oauth_error=invalid_provider");
            return;
        }

        User user = provisioningService.provisionGoogleUser(oidcUser);

        if (!Boolean.TRUE.equals(user.getIsActive())
                || user.getDeletedAt() != null) {
            response.sendRedirect("/login?oauth_error=account_disabled");
            return;
        }

        String sessionId = jwtService.createSession(user);

        String accessToken =
                jwtService.generateToken(user, sessionId);

        String refreshToken =
                refreshTokenService.issue(user, sessionId);

        response.addHeader(
                "Set-Cookie",
                authCookieFactory
                        .buildAuthCookie(accessToken)
                        .toString()
        );

        response.addHeader(
                "Set-Cookie",
                authCookieFactory
                        .buildRefreshCookie(refreshToken)
                        .toString()
        );

        String next = getSafeNext(request);

        invalidateOAuthSession(request);

        response.sendRedirect(next);
    }

    private String getSafeNext(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            return "/";
        }

        Object value =
                session.getAttribute(NEXT_SESSION_ATTRIBUTE);

        if (!(value instanceof String next)
                || next.isBlank()) {
            return "/";
        }

        return isSafeRelativePath(next)
                ? next
                : "/";
    }

    private boolean isSafeRelativePath(String value) {
        return value.startsWith("/")
                && !value.startsWith("//")
                && !value.contains("\\")
                && !value.contains("://");
    }

    private void invalidateOAuthSession(
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }
    }
}