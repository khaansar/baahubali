CREATE TABLE products (
  id BINARY(16) NOT NULL PRIMARY KEY,
  product_type VARCHAR(32) NOT NULL,
  reference_id BINARY(16) NOT NULL,          -- external id, NO cross-service FK
  name VARCHAR(255) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  deleted_at DATETIME(6) NULL,
  CONSTRAINT uq_product_ref UNIQUE (product_type, reference_id),
  CONSTRAINT ck_product_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE product_prices (
  id BINARY(16) NOT NULL PRIMARY KEY,
  product_id BINARY(16) NOT NULL,
  currency CHAR(3) NOT NULL,
  amount_minor BIGINT NOT NULL,
  effective_from DATETIME(6) NOT NULL,
  effective_until DATETIME(6) NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_price_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT ck_price_amount CHECK (amount_minor >= 0),
  INDEX idx_price_lookup (product_id, currency, status, effective_from)
);