-- Baseline schema for payment-service.
-- Squashed from all earlier migrations; equivalent to running them in order on an empty database.

CREATE TABLE `audit_logs` (
  `id` binary(16) NOT NULL,
  `actor_type` varchar(16) NOT NULL,
  `actor_id` varchar(64) DEFAULT NULL,
  `action` varchar(48) NOT NULL,
  `aggregate_type` varchar(24) NOT NULL,
  `aggregate_id` varchar(64) NOT NULL,
  `order_id` binary(16) DEFAULT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `details` text,
  `source_ip` varchar(64) DEFAULT NULL,
  `correlation_id` varchar(64) DEFAULT NULL,
  `occurred_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_order` (`order_id`,`occurred_at`),
  KEY `idx_audit_aggregate` (`aggregate_type`,`aggregate_id`)
) ENGINE=InnoDB;

CREATE TABLE `coupons` (
  `id` binary(16) NOT NULL,
  `code` varchar(64) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `discount_type` varchar(16) NOT NULL,
  `discount_value` bigint NOT NULL,
  `minimum_order_amount` bigint NOT NULL DEFAULT '0',
  `maximum_discount_amount` bigint DEFAULT NULL,
  `starts_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `usage_limit` int DEFAULT NULL,
  `per_user_usage_limit` int DEFAULT NULL,
  `used_count` int NOT NULL DEFAULT '0',
  `stackable` tinyint(1) NOT NULL DEFAULT '0',
  `first_order_only` tinyint(1) NOT NULL DEFAULT '0',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_coupon_code` (`code`),
  CONSTRAINT `ck_coupon_pct` CHECK (((`discount_type` <> _utf8mb4'PERCENTAGE') or (`discount_value` <= 10000))),
  CONSTRAINT `ck_coupon_used` CHECK (((`usage_limit` is null) or (`used_count` <= `usage_limit`))),
  CONSTRAINT `ck_coupon_value` CHECK ((`discount_value` > 0))
) ENGINE=InnoDB;

CREATE TABLE `products` (
  `id` binary(16) NOT NULL,
  `product_type` varchar(32) NOT NULL,
  `reference_id` binary(16) NOT NULL,
  `name` varchar(255) NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_product_ref` (`product_type`,`reference_id`),
  CONSTRAINT `ck_product_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'INACTIVE')))
) ENGINE=InnoDB;

CREATE TABLE `coupon_products` (
  `coupon_id` binary(16) NOT NULL,
  `product_id` binary(16) NOT NULL,
  PRIMARY KEY (`coupon_id`,`product_id`),
  KEY `idx_cp_product` (`product_id`),
  CONSTRAINT `fk_cp_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `coupons` (`id`),
  CONSTRAINT `fk_cp_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `orders` (
  `id` binary(16) NOT NULL,
  `order_number` varchar(32) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `currency` char(3) NOT NULL,
  `subtotal_amount` bigint NOT NULL,
  `discount_amount` bigint NOT NULL DEFAULT '0',
  `taxable_amount` bigint NOT NULL DEFAULT '0',
  `tax_amount` bigint NOT NULL DEFAULT '0',
  `total_amount` bigint NOT NULL,
  `coupon_code` varchar(64) DEFAULT NULL,
  `status` varchar(24) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_order_number` (`order_number`),
  KEY `idx_orders_user` (`user_id`,`created_at`),
  KEY `idx_orders_status_expiry` (`status`,`expires_at`),
  CONSTRAINT `ck_order_amounts` CHECK (((`subtotal_amount` >= 0) and (`discount_amount` >= 0) and (`tax_amount` >= 0) and (`total_amount` >= 0) and (`discount_amount` <= `subtotal_amount`)))
) ENGINE=InnoDB;

CREATE TABLE `coupon_redemptions` (
  `id` binary(16) NOT NULL,
  `coupon_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `order_id` binary(16) NOT NULL,
  `status` varchar(16) NOT NULL,
  `discount_amount` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_cr_order` (`order_id`),
  KEY `idx_cr_coupon_user` (`coupon_id`,`user_id`,`status`),
  CONSTRAINT `fk_cr_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `coupons` (`id`),
  CONSTRAINT `fk_cr_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `coupon_user_usage` (
  `coupon_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `used_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`coupon_id`,`user_id`),
  CONSTRAINT `fk_cuu_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `coupons` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `entitlements` (
  `id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `product_type` varchar(32) NOT NULL,
  `product_reference_id` binary(16) NOT NULL,
  `source` varchar(16) NOT NULL,
  `source_order_id` binary(16) DEFAULT NULL,
  `status` varchar(16) NOT NULL,
  `granted_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `revoke_reason` varchar(255) DEFAULT NULL,
  `active_key` tinyint GENERATED ALWAYS AS ((case when (`status` = _utf8mb4'ACTIVE') then 1 else NULL end)) STORED,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_ent_active` (`user_id`,`product_type`,`product_reference_id`,`active_key`),
  KEY `idx_ent_user_status` (`user_id`,`status`),
  KEY `idx_ent_order` (`source_order_id`),
  CONSTRAINT `fk_ent_order` FOREIGN KEY (`source_order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `ck_ent_purchase_trace` CHECK (((`source` <> _utf8mb4'PURCHASE') or (`source_order_id` is not null)))
) ENGINE=InnoDB;

CREATE TABLE `idempotency_keys` (
  `id` binary(16) NOT NULL,
  `idem_key` varchar(80) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `operation` varchar(40) NOT NULL,
  `request_hash` char(64) NOT NULL,
  `status` varchar(16) NOT NULL,
  `response_status` int DEFAULT NULL,
  `response_body` mediumtext,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_idem` (`user_id`,`operation`,`idem_key`),
  KEY `idx_idem_expiry` (`expires_at`)
) ENGINE=InnoDB;

CREATE TABLE `order_items` (
  `id` binary(16) NOT NULL,
  `order_id` binary(16) NOT NULL,
  `product_id` binary(16) NOT NULL,
  `product_type` varchar(32) NOT NULL,
  `product_reference_id` binary(16) NOT NULL,
  `product_name_snapshot` varchar(255) NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `unit_price_amount` bigint NOT NULL,
  `discount_amount` bigint NOT NULL DEFAULT '0',
  `tax_amount` bigint NOT NULL DEFAULT '0',
  `final_amount` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_items_order` (`order_id`),
  KEY `idx_items_product_ref` (`product_type`,`product_reference_id`),
  CONSTRAINT `fk_item_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `outbox_events` (
  `id` binary(16) NOT NULL,
  `topic` varchar(64) NOT NULL,
  `aggregate_type` varchar(32) NOT NULL,
  `aggregate_id` varchar(64) NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `payload` mediumtext NOT NULL,
  `status` varchar(12) NOT NULL DEFAULT 'PENDING',
  `attempts` int NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_outbox_pending` (`status`,`created_at`)
) ENGINE=InnoDB;

CREATE TABLE `payment_webhook_events` (
  `id` binary(16) NOT NULL,
  `provider` varchar(24) NOT NULL,
  `provider_event_id` varchar(80) NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `payload` mediumtext NOT NULL,
  `signature_valid` tinyint(1) NOT NULL,
  `processed` tinyint(1) NOT NULL DEFAULT '0',
  `processed_at` datetime(6) DEFAULT NULL,
  `attempts` int NOT NULL DEFAULT '0',
  `last_error` varchar(500) DEFAULT NULL,
  `received_at` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_webhook` (`provider`,`provider_event_id`),
  KEY `idx_webhook_unprocessed` (`processed`,`received_at`)
) ENGINE=InnoDB;

CREATE TABLE `payments` (
  `id` binary(16) NOT NULL,
  `order_id` binary(16) NOT NULL,
  `provider` varchar(24) NOT NULL,
  `provider_order_id` varchar(64) NOT NULL,
  `provider_payment_id` varchar(64) DEFAULT NULL,
  `provider_status` varchar(32) DEFAULT NULL,
  `amount` bigint NOT NULL,
  `currency` char(3) NOT NULL,
  `status` varchar(24) NOT NULL,
  `method` varchar(24) DEFAULT NULL,
  `failure_code` varchar(64) DEFAULT NULL,
  `failure_reason` varchar(255) DEFAULT NULL,
  `authorized_at` datetime(6) DEFAULT NULL,
  `captured_at` datetime(6) DEFAULT NULL,
  `refunded_amount` bigint NOT NULL DEFAULT '0',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_provider_order` (`provider`,`provider_order_id`),
  UNIQUE KEY `uq_provider_payment` (`provider`,`provider_payment_id`),
  KEY `idx_payments_order` (`order_id`),
  KEY `idx_payments_status_created` (`status`,`created_at`),
  CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `ck_refunded` CHECK (((`refunded_amount` >= 0) and (`refunded_amount` <= `amount`)))
) ENGINE=InnoDB;

CREATE TABLE `product_prices` (
  `id` binary(16) NOT NULL,
  `product_id` binary(16) NOT NULL,
  `currency` char(3) NOT NULL,
  `amount_minor` bigint NOT NULL,
  `effective_from` datetime(6) NOT NULL,
  `effective_until` datetime(6) DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_price_lookup` (`product_id`,`currency`,`status`,`effective_from`),
  CONSTRAINT `fk_price_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `ck_price_amount` CHECK ((`amount_minor` >= 0))
) ENGINE=InnoDB;

CREATE TABLE `refunds` (
  `id` binary(16) NOT NULL,
  `payment_id` binary(16) NOT NULL,
  `order_id` binary(16) NOT NULL,
  `amount` bigint NOT NULL,
  `currency` char(3) NOT NULL,
  `reason` varchar(500) NOT NULL,
  `status` varchar(16) NOT NULL,
  `provider_refund_id` varchar(64) DEFAULT NULL,
  `idempotency_key` varchar(80) NOT NULL,
  `requested_by` binary(16) NOT NULL,
  `requested_at` datetime(6) NOT NULL,
  `processed_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_refund_idem` (`payment_id`,`idempotency_key`),
  UNIQUE KEY `uq_provider_refund` (`provider_refund_id`),
  KEY `fk_refund_order` (`order_id`),
  KEY `idx_refund_payment` (`payment_id`),
  KEY `idx_refund_status` (`status`,`updated_at`),
  CONSTRAINT `fk_refund_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_refund_payment` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`),
  CONSTRAINT `ck_refund_amount` CHECK ((`amount` > 0))
) ENGINE=InnoDB;
