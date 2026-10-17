package com.example.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class NotificationRecipientResolver {

    private final RestClient iamClient;

    public NotificationRecipientResolver(
            RestClient.Builder builder,
            @Value("${internal.services.iam-url}") String iamUrl,
            @Value("${internal.auth.my-secret}") String serviceSecret
    ) {
        this.iamClient = builder
                .baseUrl(iamUrl)
                .defaultHeader("X-Service-Caller", "notification-service")
                .defaultHeader("X-Service-Auth", serviceSecret)
                .build();
    }

    public Contact resolve(String userId) {
        try {
            JsonNode response = iamClient.get()
                    .uri("/auth-api/internal/users/{userId}/notification-contact", userId)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode data = response == null
                    ? null
                    : response.path("data");

            if (data == null || data.isMissingNode() || data.isNull()) {
                return null;
            }

            String email = data.path("email").asText(null);
            String firstName = data.path("firstName").asText(null);
            boolean emailVerified = data.path("emailVerified").asBoolean(false);

            if (email == null || email.isBlank() || !emailVerified) {
                return null;
            }

            return new Contact(email, firstName);

        } catch (RestClientException e) {
            log.error(
                    "Failed to resolve notification contact for userId={}",
                    userId,
                    e
            );
            return null;
        }
    }

    public record Contact(String email, String firstName) {
    }
}