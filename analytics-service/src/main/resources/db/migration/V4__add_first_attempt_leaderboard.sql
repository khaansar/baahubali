ALTER TABLE test_reports
    ADD COLUMN started_at DATETIME(6) NULL AFTER test_id;

CREATE INDEX idx_test_reports_first_attempt_leaderboard
    ON test_reports (test_id, user_id, started_at, created_at, attempt_id, status);
