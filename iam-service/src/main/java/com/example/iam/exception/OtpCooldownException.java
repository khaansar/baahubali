package com.example.iam.exception;

public class OtpCooldownException extends RuntimeException {

    private final long retryAfterSeconds;

    public OtpCooldownException(long retryAfterSeconds) {
        super("A code was sent recently. Please wait " + retryAfterSeconds + " seconds before requesting another.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
