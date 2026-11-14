package com.example.iam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * One-time codes emailed for email verification and account deletion.
 *
 * @param ttl            how long a code stays valid
 * @param maxAttempts    wrong entries allowed before the code is discarded
 * @param resendCooldown minimum wait before another code can be sent to the same user
 */
@ConfigurationProperties(prefix = "app.otp")
public record OtpProperties(
        Duration ttl,
        int maxAttempts,
        Duration resendCooldown
) {
    public OtpProperties {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("app.otp.ttl must be a positive duration, e.g. 10m");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("app.otp.max-attempts must be at least 1");
        }
        if (resendCooldown == null || resendCooldown.isNegative()) {
            throw new IllegalArgumentException("app.otp.resend-cooldown must be zero or a positive duration, e.g. 60s");
        }
        if (resendCooldown.compareTo(ttl) >= 0) {
            throw new IllegalArgumentException("app.otp.resend-cooldown must be shorter than app.otp.ttl");
        }
    }
}
