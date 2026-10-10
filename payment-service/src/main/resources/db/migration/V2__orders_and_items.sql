CREATE TABLE orders (
  id BINARY(16) NOT NULL PRIMARY KEY,
  order_number VARCHAR(32) NOT NULL,
  user_id BINARY(16) NOT NULL,
  currency CHAR(3) NOT NULL,
  subtotal_amount BIGINT NOT NULL,
  discount_amount BIGINT NOT NULL DEFAULT 0,
  taxable_amount BIGINT NOT NULL DEFAULT 0,
  tax_amount BIGINT NOT NULL DEFAULT 0,
  total_amount BIGINT NOT NULL,
  coupon_code VARCHAR(64) NULL,
  status VARCHAR(24) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  paid_at DATETIME(6) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_order_number UNIQUE (order_number),
  CONSTRAINT ck_order_amounts CHECK (subtotal_amount >= 0 AND discount_amount >= 0
      AND tax_amount >= 0 AND total_amount >= 0 AND discount_amount <= subtotal_amount),
  INDEX idx_orders_user (user_id, created_at),
  INDEX idx_orders_status_expiry (status, expires_at)
);

CREATE TABLE order_items (
  id BINARY(16) NOT NULL PRIMARY KEY,
  order_id BINARY(16) NOT NULL,
  product_id BINARY(16) NOT NULL,
  product_type VARCHAR(32) NOT NULL,
  product_reference_id BINARY(16) NOT NULL,
  product_name_snapshot VARCHAR(255) NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  unit_price_amount BIGINT NOT NULL,
  discount_amount BIGINT NOT NULL DEFAULT 0,
  tax_amount BIGINT NOT NULL DEFAULT 0,
  final_amount BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES orders(id),
  INDEX idx_items_order (order_id),
  INDEX idx_items_product_ref (product_type, product_reference_id)
);