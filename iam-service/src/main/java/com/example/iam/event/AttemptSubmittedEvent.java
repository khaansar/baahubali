package com.example.iam.event;

import java.time.Instant;

public record AttemptSubmittedEvent(String userId, String testId, Instant timestamp) { }
