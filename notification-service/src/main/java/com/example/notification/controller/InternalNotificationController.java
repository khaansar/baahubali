package com.example.notification.controller;

import com.example.notification.entity.Notification;
import com.example.notification.provider.NotificationProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationProvider emailProvider;

    @PostMapping("/email/test")
    public ResponseEntity<Void> sendTestEmail(
            @Valid @RequestBody TestEmailRequest request
    ) {
        Notification notification = Notification.builder()
                .id("smtp-test")
                .eventId("smtp-test")
                .eventType("SMTP_TEST")
                .channel(Notification.Channel.EMAIL)
                .recipient(request.recipient())
                .templateCode("SMTP_TEST_EMAIL_V1")
                .payload("{}")
                .status(Notification.Status.PROCESSING)
                .attemptCount(1)
                .build();

        NotificationProvider.DeliveryResult result = emailProvider.send(
                notification,
                request.subject(),
                request.body()
        );

        if (!result.successful()) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.accepted().build();
    }

    public record TestEmailRequest(
            @NotBlank
            @Email
            String recipient,

            @NotBlank
            String subject,

            @NotBlank
            String body
    ) {
    }
}
