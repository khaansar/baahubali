ALTER TABLE reviews
    ADD COLUMN moderation_reason TEXT NULL,
    ADD COLUMN moderated_by VARCHAR(255) NULL,
    ADD COLUMN moderated_at TIMESTAMP(6) NULL;

ALTER TABLE reviews
    ALTER COLUMN status SET DEFAULT 'PENDING';

CREATE INDEX idx_reviews_status_created_at ON reviews(status, created_at);
CREATE INDEX idx_reviews_deleted_at ON reviews(deleted_at);
