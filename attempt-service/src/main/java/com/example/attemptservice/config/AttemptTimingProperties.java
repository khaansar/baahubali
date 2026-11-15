package com.example.attemptservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Timing tolerances for attempts.
 *
 * <p>{@code app.attempt.save-grace-seconds} (ATTEMPT_SAVE_GRACE_SECONDS, default 10): how long after
 * the attempt or section deadline an answer save is still accepted, so an answer chosen in the last
 * moment is not lost to autosave delay and network latency. Attempts still end at the real
 * deadline; expiry sweeps wait for the grace period to pass before finalizing.
 */
@Component
public class AttemptTimingProperties {

    static final long MAX_SAVE_GRACE_SECONDS = 120;

    private final long saveGraceSeconds;

    public AttemptTimingProperties(@Value("${app.attempt.save-grace-seconds:10}") long saveGraceSeconds) {
        if (saveGraceSeconds < 0 || saveGraceSeconds > MAX_SAVE_GRACE_SECONDS) {
            throw new IllegalStateException(
                    "app.attempt.save-grace-seconds (ATTEMPT_SAVE_GRACE_SECONDS) must be between 0 and "
                            + MAX_SAVE_GRACE_SECONDS + ", but was " + saveGraceSeconds);
        }
        this.saveGraceSeconds = saveGraceSeconds;
    }

    public long saveGraceSeconds() {
        return saveGraceSeconds;
    }
}
