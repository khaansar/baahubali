package com.example.analyticsservice.core;

/** Event is missing data required by the analytics contract. Not retryable. */
public class InvalidAttemptEventException extends RuntimeException {

    public InvalidAttemptEventException(String message) {
        super(message);
    }
}