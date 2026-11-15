package com.example.iam.service;

import com.example.iam.entity.OAuthProvider;
import com.example.iam.entity.User;
import com.example.iam.repository.UserIdentityRepository;
import com.example.iam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2UserProvisioningServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserIdentityRepository userIdentityRepository;
    @Mock private OidcUser oidcUser;

    @InjectMocks private OAuth2UserProvisioningService service;

    @BeforeEach
    void setUp() {
        when(oidcUser.getSubject()).thenReturn("google-subject");
        when(oidcUser.getEmail()).thenReturn("Student@Example.com");
        when(oidcUser.getClaimAsBoolean("email_verified")).thenReturn(true);
        when(userIdentityRepository.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, "google-subject"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
    }

    @Test
    void overLongGooglePictureIsSkippedInsteadOfFailingSignUp() {
        when(oidcUser.getGivenName()).thenReturn("Asha");
        when(oidcUser.getPicture()).thenReturn("https://lh3.googleusercontent.com/a/" + "x".repeat(600));

        User user = service.provisionGoogleUser(oidcUser);

        assertThat(user.getAvatarUrl()).isNull();
        assertThat(user.getFirstName()).isEqualTo("Asha");
    }

    @Test
    void normalGooglePictureAndOverLongNamesAreStoredWithinColumnSizes() {
        String picture = "https://lh3.googleusercontent.com/a/photo=s96-c";
        when(oidcUser.getGivenName()).thenReturn("A".repeat(150));
        when(oidcUser.getFamilyName()).thenReturn("B".repeat(150));
        when(oidcUser.getPicture()).thenReturn(picture);

        service.provisionGoogleUser(oidcUser);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getAvatarUrl()).isEqualTo(picture);
        assertThat(saved.getValue().getFirstName()).hasSize(100);
        assertThat(saved.getValue().getLastName()).hasSize(100);
    }
}
