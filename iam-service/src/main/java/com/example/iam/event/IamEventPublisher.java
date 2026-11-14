package com.example.iam.event;

import com.example.iam.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class IamEventPublisher {

    private static final String IAM_EVENTS_TOPIC = "iam-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public static final String EMAIL_VERIFICATION_OTP_REQUESTED = "USER_EMAIL_VERIFICATION_OTP_REQUESTED";
    public static final String ACCOUNT_DELETION_OTP_REQUESTED = "ACCOUNT_DELETION_OTP_REQUESTED";

    /** Asks the notification service to email a one-time code to the user. */
    public void publishOtpRequested(
            User user,
            String eventType,
            String otp,
            long expiresInMinutes
    ) {
        OtpRequestedEvent event =
                new OtpRequestedEvent(
                        UUID.randomUUID().toString(),
                        eventType,
                        1,
                        Instant.now(),
                        "iam-service",
                        user.getId().toString(),
                        null,
                        user.getEmail(),
                        Map.of(
                                "channel", "EMAIL",
                                "firstName", user.getFirstName(),
                                "otp", otp,
                                "expiresInMinutes", expiresInMinutes
                        )
                );

        try {
            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    IAM_EVENTS_TOPIC,
                    user.getId().toString(),
                    message
            ).whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error(
                            "Failed to publish {} event userId={}",
                            eventType,
                            user.getId(),
                            exception
                    );
                    return;
                }

                log.debug(
                        "Published {} event userId={}",
                        eventType,
                        user.getId()
                );
            });

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to serialize " + eventType + " event",
                    e
            );
        }
    }

    public void publishPasswordResetRequested(
            User user,
            String resetUrl
    ) {
        PasswordResetRequestedEvent event =
                new PasswordResetRequestedEvent(
                        UUID.randomUUID().toString(),
                        "PASSWORD_RESET_REQUESTED",
                        1,
                        Instant.now(),
                        "iam-service",
                        user.getId().toString(),
                        null,
                        user.getEmail(),
                        Map.of(
                                "channel", "EMAIL",
                                "firstName", user.getFirstName(),
                                "resetUrl", resetUrl
                        )
                );

        try {
            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    IAM_EVENTS_TOPIC,
                    user.getId().toString(),
                    message
            ).whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error(
                            "Failed to publish password reset event userId={}",
                            user.getId(),
                            exception
                    );
                    return;
                }

                log.debug(
                        "Published password reset event userId={}",
                        user.getId()
                );
            });

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to serialize password reset event",
                    e
            );
        }
    }
}