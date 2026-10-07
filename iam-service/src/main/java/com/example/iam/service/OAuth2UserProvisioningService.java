package com.example.iam.service;

import com.example.iam.entity.OAuthProvider;
import com.example.iam.entity.Role;
import com.example.iam.entity.User;
import com.example.iam.entity.UserIdentity;
import com.example.iam.repository.UserIdentityRepository;
import com.example.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OAuth2UserProvisioningService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;

    @Transactional
    public User provisionGoogleUser(OidcUser oidcUser) {

        String providerSubject = oidcUser.getSubject();

        if (providerSubject == null || providerSubject.isBlank()) {
            throw new IllegalStateException(
                    "Google account does not contain a valid subject"
            );
        }

        String email = oidcUser.getEmail();

        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                    "Google account does not provide an email address"
            );
        }

        Boolean emailVerified = oidcUser.getClaimAsBoolean("email_verified");

        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new IllegalStateException(
                    "Google email address is not verified"
            );
        }

        final String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        return userIdentityRepository
                .findByProviderAndProviderSubject(
                        OAuthProvider.GOOGLE,
                        providerSubject
                )
                .map(identity -> userRepository.findById(identity.getUserId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "OAuth identity points to a missing user"
                                )))
                .orElseGet(() -> createOrLinkGoogleUser(
                        oidcUser,
                        normalizedEmail,
                        providerSubject
                ));
    }

    private User createOrLinkGoogleUser(
            OidcUser oidcUser,
            String email,
            String providerSubject) {

        User user = userRepository.findByEmail(email)
                .map(existingUser -> {

                    if (!Boolean.TRUE.equals(existingUser.getEmailVerified())) {
                        throw new IllegalStateException(
                                "An existing account with this email must be verified before Google can be linked"
                        );
                    }

                    return existingUser;
                })
                .orElseGet(() -> createGoogleUser(oidcUser, email));

        UserIdentity identity = UserIdentity.builder()
                .userId(user.getId())
                .provider(OAuthProvider.GOOGLE)
                .providerSubject(providerSubject)
                .email(email)
                .build();

        userIdentityRepository.save(identity);

        return user;
    }

    private User createGoogleUser(
            OidcUser oidcUser,
            String email) {

        String firstName = oidcUser.getGivenName();

        if (firstName == null || firstName.isBlank()) {
            firstName = oidcUser.getFullName();

            if (firstName == null || firstName.isBlank()) {
                firstName = "User";
            }
        }

        String lastName = oidcUser.getFamilyName();

        return userRepository.save(
                User.builder()
                        .firstName(firstName)
                        .lastName(lastName)
                        .email(email)
                        .passwordHash(null)
                        .role(Role.STUDENT)
                        .avatarUrl(oidcUser.getPicture())
                        .emailVerified(true)
                        .build()
        );
    }
}