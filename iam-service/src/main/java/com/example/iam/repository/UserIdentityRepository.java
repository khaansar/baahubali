package com.example.iam.repository;

import com.example.iam.entity.OAuthProvider;
import com.example.iam.entity.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

    Optional<UserIdentity> findByProviderAndProviderSubject(
            OAuthProvider provider,
            String providerSubject
    );

    Optional<UserIdentity> findByUserIdAndProvider(
            UUID userId,
            OAuthProvider provider
    );
}