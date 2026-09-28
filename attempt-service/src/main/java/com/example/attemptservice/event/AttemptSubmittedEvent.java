package com.example.attemptservice.event;

import java.io.Serializable;
import java.time.Instant;

/** Event published exactly when an attempt reaches the SUBMITTED state. */
public record AttemptSubmittedEvent(String userId, String testId, Instant timestamp) implements Serializable { }
