CREATE TABLE active_attempts (
    user_id VARCHAR(64) NOT NULL,
    test_id VARCHAR(64) NOT NULL,
    attempt_id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, test_id),
    UNIQUE KEY uk_active_attempt_id (attempt_id)
);

-- Protect active attempts that existed before this migration.  New writes use
-- the table above atomically; duplicate legacy rows are intentionally left for
-- an operator review instead of silently choosing one.
INSERT IGNORE INTO active_attempts (user_id, test_id, attempt_id, created_at)
SELECT user_id, test_id, id, started_at
FROM attempts
WHERE status = 'IN_PROGRESS';
