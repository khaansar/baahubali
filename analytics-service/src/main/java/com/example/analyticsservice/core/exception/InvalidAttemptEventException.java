package com.example.analyticsservice.core.exception;

import com.example.analyticsservice.core.exception.*;

import com.example.analyticsservice.core.entity.*;
import com.example.analyticsservice.core.repository.*;

/** Event is missing data required by the analytics contract. Not retryable. */
public class InvalidAttemptEventException extends RuntimeException {

    public InvalidAttemptEventException(String message) {
        super(message);
    }
}