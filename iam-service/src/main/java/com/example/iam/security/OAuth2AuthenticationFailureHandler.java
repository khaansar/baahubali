package com.example.iam.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler
        implements AuthenticationFailureHandler {

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException, ServletException {

        /*
         * IMPORTANT:
         * Do not hide the actual OAuth failure while debugging.
         */
        System.err.println(
                "OAuth2 authentication failed: "
                        + exception.getClass().getName()
                        + " - "
                        + exception.getMessage()
        );

        if (exception.getCause() != null) {
            System.err.println(
                    "OAuth2 authentication failure cause: "
                            + exception.getCause().getClass().getName()
                            + " - "
                            + exception.getCause().getMessage()
            );
        }

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        String errorMessage = URLEncoder.encode(
                "Google authentication failed",
                StandardCharsets.UTF_8
        );

        String redirectUrl =
                buildFrontendUrl(
                        "/login?oauth_error=authentication_failed"
                                + "&oauth_message="
                                + errorMessage
                );

        response.sendRedirect(redirectUrl);
    }

    private String buildFrontendUrl(String path) {

        String base = frontendUrl;

        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return base + path;
    }
}