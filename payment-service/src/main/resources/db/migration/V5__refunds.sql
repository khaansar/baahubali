CREATE TABLE coupon_redemptions (
  id BINARY(16) NOT NULL PRIMARY KEY,
  coupon_id BINARY(16) NOT NULL,
  user_id BINARY(16) NOT NULL,
  order_id BINARY(16) NOT NULL,
  status VARCHAR(16) NOT NULL,               -- RESERVED, CONFIRMED, RELEASED
  discount_amount BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_cr_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id),
  CONSTRAINT fk_cr_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT uq_cr_order UNIQUE (order_id),
  INDEX idx_cr_coupon_user (coupon_id, user_id, status)
);

CREATE TABLE refunds (
  id BINARY(16) NOT NULL PRIMARY KEY,
  payment_id BINARY(16) NOT NULL,
  order_id BINARY(16) NOT NULL,
  amount BIGINT NOT NULL,
  currency CHAR(3) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(16) NOT NULL,
  provider_refund_id VARCHAR(64) NULL,
  idempotency_key VARCHAR(80) NOT NULL,
  requested_by BINARY(16) NOT NULL,
  requested_at DATETIME(6) NOT NULL,
  processed_at DATETIME(6) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
  CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT ck_refund_amount CHECK (amount > 0),
  CONSTRAINT uq_refund_idem UNIQUE (payment_id, idempotency_key),
  CONSTRAINT uq_provider_refund UNIQUE (provider_refund_id),
  INDEX idx_refund_payment (payment_id),
  INDEX idx_refund_status (status, updated_at)
);