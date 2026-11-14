-- Baseline schema for analytics-service.
-- Squashed from all earlier migrations; equivalent to running them in order on an empty database.

CREATE TABLE `section_performance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attempt_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `test_id` varchar(36) NOT NULL,
  `section_id` varchar(36) NOT NULL,
  `section_name` varchar(100) NOT NULL,
  `total_questions` int NOT NULL DEFAULT '0',
  `correct_count` int NOT NULL DEFAULT '0',
  `incorrect_count` int NOT NULL DEFAULT '0',
  `unattempted_count` int NOT NULL DEFAULT '0',
  `accuracy_percentage` decimal(5,2) NOT NULL DEFAULT '0.00',
  `time_spent_seconds` int DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_attempt` (`attempt_id`),
  KEY `idx_user_section` (`user_id`,`section_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `test_attempts` (
  `attempt_id` varchar(36) NOT NULL,
  `test_id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL,
  `started_at` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`attempt_id`),
  KEY `idx_test_attempts_user_first` (`test_id`,`user_id`,`started_at`,`attempt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `test_reports` (
  `id` varchar(36) NOT NULL,
  `attempt_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `category_id` varchar(36) NOT NULL,
  `test_series_id` varchar(36) NOT NULL,
  `test_id` varchar(36) NOT NULL,
  `started_at` datetime(6) DEFAULT NULL,
  `total_score` decimal(6,2) NOT NULL DEFAULT '0.00',
  `max_score` decimal(6,2) NOT NULL DEFAULT '0.00',
  `accuracy_percentage` decimal(5,2) NOT NULL DEFAULT '0.00',
  `time_taken_seconds` int NOT NULL DEFAULT '0',
  `percentile` decimal(5,2) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PROCESSING',
  `report_data` json DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `attempt_id` (`attempt_id`),
  KEY `idx_user_test` (`user_id`,`test_id`),
  KEY `idx_user_series` (`user_id`,`test_series_id`),
  KEY `idx_user_category` (`user_id`,`category_id`),
  KEY `idx_test_status` (`test_id`,`status`),
  KEY `idx_test_reports_first_attempt_leaderboard` (`test_id`,`user_id`,`started_at`,`created_at`,`attempt_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_topic_performance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(36) NOT NULL,
  `topic` varchar(100) NOT NULL,
  `total_questions` int NOT NULL DEFAULT '0',
  `correct_count` int NOT NULL DEFAULT '0',
  `incorrect_count` int NOT NULL DEFAULT '0',
  `unattempted_count` int NOT NULL DEFAULT '0',
  `accuracy_percentage` decimal(5,2) NOT NULL DEFAULT '0.00',
  `attempts_count` int NOT NULL DEFAULT '0',
  `last_attempt_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_topic` (`user_id`,`topic`),
  KEY `idx_user_topic_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
