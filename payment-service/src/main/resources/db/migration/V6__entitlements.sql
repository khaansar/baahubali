CREATE TABLE entitlements (
  id BINARY(16) NOT NULL PRIMARY KEY,
  user_id BINARY(16) NOT NULL,
  product_type VARCHAR(32) NOT NULL,
  product_reference_id BINARY(16) NOT NULL,
  source VARCHAR(16) NOT NULL,
  source_order_id BINARY(16) NULL,
  status VARCHAR(16) NOT NULL,
  granted_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NULL,
  revoked_at DATETIME(6) NULL,
  revoke_reason VARCHAR(255) NULL,
  -- MySQL has no partial unique index: NULL when not ACTIVE, so only ACTIVE rows collide.
  active_key TINYINT AS (CASE WHEN status = 'ACTIVE' THEN 1 ELSE NULL END) STORED,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_ent_order FOREIGN KEY (source_order_id) REFERENCES orders(id),
  CONSTRAINT uq_ent_active UNIQUE (user_id, product_type, product_reference_id, active_key),
  CONSTRAINT ck_ent_purchase_trace CHECK (source <> 'PURCHASE' OR source_order_id IS NOT NULL),
  INDEX idx_ent_user_status (user_id, status),
  INDEX idx_ent_order (source_order_id)
);