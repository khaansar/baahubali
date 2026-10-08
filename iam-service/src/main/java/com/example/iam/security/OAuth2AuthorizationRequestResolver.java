package com.example.iam.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthorizationRequestResolver
        implements org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public OAuth2AuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {

        this.delegate =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        "/oauth2/authorization"
                );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(
            HttpServletRequest request) {

        return resolveAndStoreNext(
                request,
                delegate.resolve(request)
        );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(
            HttpServletRequest request,
            String registrationId) {

        return resolveAndStoreNext(
                request,
                delegate.resolve(request, registrationId)
        );
    }

    private OAuth2AuthorizationRequest resolveAndStoreNext(
            HttpServletRequest request,
            OAuth2AuthorizationRequest authorizationRequest) {

        if (authorizationRequest == null) {
            return null;
        }

        String next = request.getParameter("next");

        if (isSafeRelativePath(next)) {

            HttpSession session = request.getSession(true);

            session.setAttribute(
                    OAuth2AuthenticationSuccessHandler.NEXT_SESSION_ATTRIBUTE,
                    next
            );
        }

        return authorizationRequest;
    }

    private boolean isSafeRelativePath(String value) {

        if (value == null || value.isBlank()) {
            return false;
        }

        return value.startsWith("/")
                && !value.startsWith("//")
                && !value.contains("\\")
                && !value.contains("://");
    }
}