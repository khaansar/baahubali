CREATE TABLE payment_product_sync_outbox (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    series_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    base_price_rupees DECIMAL(10,2) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT ck_payment_product_sync_outbox_status CHECK (status IN ('PENDING', 'SYNCED', 'FAILED')),
    CONSTRAINT ck_payment_product_sync_outbox_attempts CHECK (attempts >= 0),
    INDEX idx_payment_product_sync_outbox_pending (status, next_attempt_at, created_at),
    INDEX idx_payment_product_sync_outbox_series (series_id, created_at)
);