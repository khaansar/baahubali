ALTER TABLE notifications
    MODIFY COLUMN recipient VARCHAR(320) NULL,
    ADD COLUMN title VARCHAR(200) NULL,
    ADD COLUMN message TEXT NULL,
    ADD COLUMN action_url VARCHAR(1000) NULL,
    ADD COLUMN read_at TIMESTAMP(6) NULL;

CREATE INDEX idx_notifications_user_channel_created
    ON notifications (user_id, channel, created_at);

CREATE INDEX idx_notifications_user_channel_read
    ON notifications (user_id, channel, read_at);