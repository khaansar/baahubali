CREATE TABLE notifications (
    id VARCHAR(36) NOT NULL,
    event_id VARCHAR(100) NOT NULL,
    user_id VARCHAR(36),
    event_type VARCHAR(100) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    recipient VARCHAR(320) NOT NULL,
    template_code VARCHAR(150) NOT NULL,
    payload LONGTEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP(6) NOT NULL,
    sent_at TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notifications_event_channel UNIQUE (event_id, channel),
    INDEX idx_notifications_user_id (user_id),
    INDEX idx_notifications_status (status),
    INDEX idx_notifications_created_at (created_at)
);

CREATE TABLE notification_attempts (
    id VARCHAR(36) NOT NULL,
    notification_id VARCHAR(36) NOT NULL,
    attempt_number INT NOT NULL,
    provider VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider_message_id VARCHAR(255),
    error_code VARCHAR(100),
    error_message TEXT,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_notification_attempts_notification_id (notification_id),
    CONSTRAINT fk_notification_attempts_notification
        FOREIGN KEY (notification_id)
        REFERENCES notifications(id)
);

CREATE TABLE notification_templates (
    id VARCHAR(36) NOT NULL,
    template_code VARCHAR(150) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    subject VARCHAR(500),
    body LONGTEXT NOT NULL,
    version INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_template_code_channel_version
        UNIQUE (template_code, channel, version),
    INDEX idx_template_code_channel (template_code, channel)
);
