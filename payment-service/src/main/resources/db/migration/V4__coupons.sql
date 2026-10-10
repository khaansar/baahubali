CREATE TABLE coupons (
  id BINARY(16) NOT NULL PRIMARY KEY,
  code VARCHAR(64) NOT NULL,                 -- stored UPPERCASE normalized
  description VARCHAR(255) NULL,
  discount_type VARCHAR(16) NOT NULL,        -- PERCENTAGE (value = basis points) | FIXED_AMOUNT (minor units)
  discount_value BIGINT NOT NULL,
  minimum_order_amount BIGINT NOT NULL DEFAULT 0,
  maximum_discount_amount BIGINT NULL,
  starts_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  usage_limit INT NULL,
  per_user_usage_limit INT NULL,
  used_count INT NOT NULL DEFAULT 0,
  stackable BOOLEAN NOT NULL DEFAULT FALSE,
  first_order_only BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_coupon_code UNIQUE (code),
  CONSTRAINT ck_coupon_value CHECK (discount_value > 0),
  CONSTRAINT ck_coupon_pct CHECK (discount_type <> 'PERCENTAGE' OR discount_value <= 10000),
  CONSTRAINT ck_coupon_used CHECK (usage_limit IS NULL OR used_count <= usage_limit)
);

CREATE TABLE coupon_products (
  coupon_id BINARY(16) NOT NULL,
  product_id BINARY(16) NOT NULL,
  PRIMARY KEY (coupon_id, product_id),
  CONSTRAINT fk_cp_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id),
  CONSTRAINT fk_cp_product FOREIGN KEY (product_id) REFERENCES products(id),
  INDEX idx_cp_product (product_id)
);

-- per-user counter row: gives us a lockable/atomic target for per-user limits
CREATE TABLE coupon_user_usage (
  coupon_id BINARY(16) NOT NULL,
  user_id BINARY(16) NOT NULL,
  used_count INT NOT NULL DEFAULT 0,
  PRIMARY KEY (coupon_id, user_id),
  CONSTRAINT fk_cuu_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id)
);