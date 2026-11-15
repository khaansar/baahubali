-- Baseline schema for test-service.
-- Squashed from all earlier migrations; equivalent to running them in order on an empty database.

CREATE TABLE `categories` (
  `id` varchar(36) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `required_languages` json NOT NULL,
  `created_by` varchar(255) NOT NULL DEFAULT 'system',
  `updated_by` varchar(255) DEFAULT NULL,
  `slug` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `uk_categories_slug` (`slug`)
) ENGINE=InnoDB;

CREATE TABLE `test_series` (
  `id` varchar(36) NOT NULL,
  `category_id` varchar(36) DEFAULT NULL,
  `base_price` decimal(10,2) NOT NULL,
  `status` varchar(20) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL DEFAULT NULL,
  `created_by` varchar(255) NOT NULL DEFAULT 'system',
  `updated_by` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `slug` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_test_series_slug` (`slug`),
  KEY `category_id` (`category_id`),
  CONSTRAINT `test_series_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `mock_tests` (
  `id` varchar(36) NOT NULL,
  `series_id` varchar(36) DEFAULT NULL,
  `duration_minutes` int NOT NULL,
  `total_marks` decimal(10,2) NOT NULL DEFAULT '0.00',
  `is_section_order_strict` tinyint(1) DEFAULT '0',
  `shuffle_sections` tinyint(1) DEFAULT '0',
  `status` varchar(20) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL DEFAULT NULL,
  `created_by` varchar(255) NOT NULL DEFAULT 'system',
  `updated_by` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `instructions` text,
  `negative_marking_enabled` tinyint(1) NOT NULL DEFAULT '0',
  `is_free` tinyint(1) NOT NULL DEFAULT '0',
  `published_at` timestamp NULL DEFAULT NULL,
  `slug` varchar(255) NOT NULL,
  `access_code` varchar(64) DEFAULT NULL,
  `window_start_time` timestamp NULL DEFAULT NULL,
  `window_end_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mock_tests_slug` (`slug`),
  KEY `series_id` (`series_id`),
  CONSTRAINT `mock_tests_ibfk_1` FOREIGN KEY (`series_id`) REFERENCES `test_series` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `payment_product_sync_outbox` (
  `id` varchar(36) NOT NULL,
  `series_id` varchar(36) NOT NULL,
  `title` varchar(255) NOT NULL,
  `active` tinyint(1) NOT NULL,
  `base_price_rupees` decimal(10,2) DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'PENDING',
  `attempts` int NOT NULL DEFAULT '0',
  `next_attempt_at` datetime(6) NOT NULL,
  `last_error` varchar(1000) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_payment_product_sync_outbox_pending` (`status`,`next_attempt_at`,`created_at`),
  KEY `idx_payment_product_sync_outbox_series` (`series_id`,`created_at`),
  CONSTRAINT `ck_payment_product_sync_outbox_attempts` CHECK ((`attempts` >= 0)),
  CONSTRAINT `ck_payment_product_sync_outbox_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'SYNCED',_utf8mb4'FAILED')))
) ENGINE=InnoDB;

CREATE TABLE `questions` (
  `id` varchar(36) NOT NULL,
  `question_type` varchar(20) DEFAULT NULL,
  `positive_marks` decimal(5,2) NOT NULL,
  `negative_marks` decimal(5,2) DEFAULT '0.00',
  `difficulty` varchar(20) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL DEFAULT NULL,
  `created_by` varchar(255) NOT NULL DEFAULT 'system',
  `updated_by` varchar(255) DEFAULT NULL,
  `correct_answer_json` json DEFAULT NULL,
  `explanation` text,
  `topic` varchar(100) DEFAULT NULL,
  `is_locked` tinyint(1) NOT NULL DEFAULT '0',
  `explanation_images` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_questions_topic` (`topic`)
) ENGINE=InnoDB;

CREATE TABLE `question_translations` (
  `id` varchar(36) NOT NULL,
  `question_id` varchar(36) NOT NULL,
  `language` varchar(10) NOT NULL,
  `question_text` text NOT NULL,
  `options_json` json DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` varchar(255) NOT NULL DEFAULT 'system',
  `updated_by` varchar(255) DEFAULT NULL,
  `question_images` json DEFAULT NULL,
  `option_images` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_qt_question_lang` (`question_id`,`language`),
  CONSTRAINT `fk_qt_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `sections` (
  `id` varchar(36) NOT NULL,
  `test_id` varchar(36) DEFAULT NULL,
  `sequence_order` int NOT NULL,
  `shuffle_questions` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` varchar(255) NOT NULL DEFAULT 'system',
  `updated_by` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `duration_minutes` int DEFAULT NULL,
  `default_negative_marks` decimal(5,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `test_id` (`test_id`),
  CONSTRAINT `sections_ibfk_1` FOREIGN KEY (`test_id`) REFERENCES `mock_tests` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `section_questions` (
  `id` varchar(36) NOT NULL,
  `section_id` varchar(36) NOT NULL,
  `question_id` varchar(36) NOT NULL,
  `sequence_order` int NOT NULL,
  `positive_marks_override` decimal(10,2) DEFAULT NULL,
  `negative_marks_override` decimal(10,2) DEFAULT NULL,
  `created_by` varchar(255) NOT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_section_question` (`section_id`,`question_id`),
  KEY `question_id` (`question_id`),
  CONSTRAINT `section_questions_ibfk_1` FOREIGN KEY (`section_id`) REFERENCES `sections` (`id`),
  CONSTRAINT `section_questions_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`)
) ENGINE=InnoDB;
