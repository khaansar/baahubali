package com.example.notification.provider;

import com.example.notification.config.NotificationProperties;
import com.example.notification.entity.Notification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SmtpEmailProvider implements EmailProvider {

    private final JavaMailSender mailSender;
    private final NotificationProperties notificationProperties;

    @Override
    public Notification.Channel getChannel() {
        return Notification.Channel.EMAIL;
    }

    @Override
    public DeliveryResult send(
            Notification notification,
            String subject,
            String body
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(
                    notificationProperties.getFrom(),
                    notificationProperties.getFromName()
            );
            helper.setTo(notification.getRecipient());
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);

            return DeliveryResult.success(
                    "SMTP",
                    UUID.randomUUID().toString()
            );

        } catch (MessagingException | UnsupportedEncodingException e) {
            return DeliveryResult.failure(
                    "SMTP",
                    "SMTP_DELIVERY_FAILED",
                    e.getMessage()
            );
        }
    }
}
