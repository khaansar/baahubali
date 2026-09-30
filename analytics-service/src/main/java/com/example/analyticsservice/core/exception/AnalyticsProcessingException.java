package com.example.analyticsservice.core.exception;

import com.example.analyticsservice.core.exception.*;

import com.example.analyticsservice.core.entity.*;
import com.example.analyticsservice.core.repository.*;

/** Processing failed after the report was claimed; the report is marked FAILED. Retryable. */
public class AnalyticsProcessingException extends RuntimeException {

    public AnalyticsProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}