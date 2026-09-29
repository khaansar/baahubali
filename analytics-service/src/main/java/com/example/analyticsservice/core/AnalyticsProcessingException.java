package com.example.analyticsservice.core;

/** Processing failed after the report was claimed; the report is marked FAILED. Retryable. */
public class AnalyticsProcessingException extends RuntimeException {

    public AnalyticsProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}