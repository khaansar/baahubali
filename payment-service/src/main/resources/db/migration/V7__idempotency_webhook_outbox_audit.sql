CREATE TABLE idempotency_keys (
  id BINARY(16) NOT NULL PRIMARY KEY,
  idem_key VARCHAR(80) NOT NULL,
  user_id BINARY(16) NOT NULL,
  operation VARCHAR(40) NOT NULL,
  request_hash CHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL,               -- IN_PROGRESS, COMPLETED, FAILED
  response_status INT NULL,
  response_body MEDIUMTEXT NULL,
  created_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_idem UNIQUE (user_id, operation, idem_key),
  INDEX idx_idem_expiry (expires_at)
);

CREATE TABLE payment_webhook_events (
  id BINARY(16) NOT NULL PRIMARY KEY,
  provider VARCHAR(24) NOT NULL,
  provider_event_id VARCHAR(80) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  payload MEDIUMTEXT NOT NULL,
  signature_valid BOOLEAN NOT NULL,
  processed BOOLEAN NOT NULL DEFAULT FALSE,
  processed_at DATETIME(6) NULL,
  attempts INT NOT NULL DEFAULT 0,
  last_error VARCHAR(500) NULL,
  received_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_webhook UNIQUE (provider, provider_event_id),
  INDEX idx_webhook_unprocessed (processed, received_at)
);

CREATE TABLE outbox_events (
  id BINARY(16) NOT NULL PRIMARY KEY,         -- == eventId (consumer dedup key)
  topic VARCHAR(64) NOT NULL,
  aggregate_type VARCHAR(32) NOT NULL,
  aggregate_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  payload MEDIUMTEXT NOT NULL,
  status VARCHAR(12) NOT NULL DEFAULT 'PENDING',
  attempts INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  published_at DATETIME(6) NULL,
  INDEX idx_outbox_pending (status, created_at)
);

CREATE TABLE audit_logs (                      -- append-only: no UPDATE/DELETE paths in code
  id BINARY(16) NOT NULL PRIMARY KEY,
  actor_type VARCHAR(16) NOT NULL,            -- USER, ADMIN, SYSTEM, PROVIDER
  actor_id VARCHAR(64) NULL,
  action VARCHAR(48) NOT NULL,
  aggregate_type VARCHAR(24) NOT NULL,
  aggregate_id VARCHAR(64) NOT NULL,
  order_id BINARY(16) NULL,
  reason VARCHAR(500) NULL,
  details TEXT NULL,
  source_ip VARCHAR(64) NULL,
  correlation_id VARCHAR(64) NULL,
  occurred_at DATETIME(6) NOT NULL,
  INDEX idx_audit_order (order_id, occurred_at),
  INDEX idx_audit_aggregate (aggregate_type, aggregate_id)
);