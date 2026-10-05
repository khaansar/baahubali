package com.example.attemptservice.entity;

import java.io.Serializable;

/** Composite primary key matching active_attempts(user_id, test_id). */
public record ActiveAttemptId(String userId, String testId) implements Serializable { }
