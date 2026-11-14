package com.example.notification.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageConversionException;
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

    /** Returns the user's verified email contact, or null when there is none to send to. */
    public Contact resolve(String userId) {
        try {
            ContactResponse response = iamClient.get()
                    .uri("/internal/users/{userId}/notification-contact", userId)
                    .retrieve()
                    .body(ContactResponse.class);

            ContactData data = response == null ? null : response.data();

            if (data == null
                    || data.email() == null
                    || data.email().isBlank()
                    || !Boolean.TRUE.equals(data.emailVerified())) {
                return null;
            }

            return new Contact(data.email(), data.firstName());

        } catch (RestClientException | HttpMessageConversionException e) {
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

    /** IAM's standard response envelope; only the payload is needed here. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContactResponse(ContactData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContactData(String email, String firstName, Boolean emailVerified) {
    }
}
