CREATE TABLE test_attempts (
    attempt_id VARCHAR(36) NOT NULL,
    test_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (attempt_id)
);

CREATE INDEX idx_test_attempts_user_first
    ON test_attempts (test_id, user_id, started_at, attempt_id);

INSERT INTO test_attempts (attempt_id, test_id, user_id, started_at)
SELECT attempt_id, test_id, user_id, COALESCE(started_at, created_at)
FROM test_reports;
