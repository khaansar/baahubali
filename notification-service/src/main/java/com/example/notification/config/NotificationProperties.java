package com.example.notification.config;

import java.time.ZoneId;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private String from;
    private String fromName;
    private String frontendBaseUrl;
    private int maxAttempts = 5;
    private long retryDelaySeconds = 30;
    /** Time zone used when printing dates (e.g. access end dates) in emails. */
    private ZoneId displayTimeZone = ZoneId.of("Asia/Kolkata");
}
