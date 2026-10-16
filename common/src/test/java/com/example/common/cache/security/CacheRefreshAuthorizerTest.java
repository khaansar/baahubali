package com.example.common.cache.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class CacheRefreshAuthorizerTest {

    @Test
    void rejectsWhenConfiguredSecretIsMissing() {
        CacheRefreshAuthorizer authorizer =
                new CacheRefreshAuthorizer("");

        assertThrows(
                ResponseStatusException.class,
                () -> authorizer.authorize("some-token")
        );
    }

    @Test
    void rejectsWhenSuppliedTokenIsMissing() {
        CacheRefreshAuthorizer authorizer =
                new CacheRefreshAuthorizer("configured-secret");

        assertThrows(
                ResponseStatusException.class,
                () -> authorizer.authorize(null)
        );

        assertThrows(
                ResponseStatusException.class,
                () -> authorizer.authorize(" ")
        );
    }

    @Test
    void rejectsIncorrectToken() {
        CacheRefreshAuthorizer authorizer =
                new CacheRefreshAuthorizer("configured-secret");

        assertThrows(
                ResponseStatusException.class,
                () -> authorizer.authorize("wrong-secret")
        );
    }

    @Test
    void acceptsMatchingToken() {
        CacheRefreshAuthorizer authorizer =
                new CacheRefreshAuthorizer("configured-secret");

        assertDoesNotThrow(
                () -> authorizer.authorize("configured-secret")
        );
    }

    @Test
    void trimsConfiguredSecretButDoesNotTrimSuppliedToken() {
        CacheRefreshAuthorizer authorizer =
                new CacheRefreshAuthorizer(" configured-secret ");

        assertDoesNotThrow(
                () -> authorizer.authorize("configured-secret")
        );

        assertThrows(
                ResponseStatusException.class,
                () -> authorizer.authorize(" configured-secret ")
        );
    }
}