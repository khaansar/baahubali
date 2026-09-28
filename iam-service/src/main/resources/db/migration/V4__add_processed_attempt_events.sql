CREATE TABLE processed_attempt_events (
    event_id VARCHAR(36) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    PRIMARY KEY (event_id)
);