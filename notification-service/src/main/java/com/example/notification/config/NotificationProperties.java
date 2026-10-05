package com.example.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private String from;
    private String fromName;
    private int maxAttempts = 5;
    private long retryDelaySeconds = 30;
}