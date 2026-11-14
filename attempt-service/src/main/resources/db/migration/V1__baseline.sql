-- Baseline schema for attempt-service.
-- Squashed from all earlier migrations; equivalent to running them in order on an empty database.

CREATE TABLE `active_attempts` (
  `user_id` varchar(64) NOT NULL,
  `test_id` varchar(64) NOT NULL,
  `attempt_id` varchar(36) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`user_id`,`test_id`),
  UNIQUE KEY `uk_active_attempt_id` (`attempt_id`)
) ENGINE=InnoDB;

CREATE TABLE `attempt_answers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attempt_id` varchar(36) NOT NULL,
  `question_id` varchar(64) NOT NULL,
  `selected_option` varchar(64) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attempt_question` (`attempt_id`,`question_id`)
) ENGINE=InnoDB;

CREATE TABLE `attempts` (
  `id` varchar(36) NOT NULL,
  `user_id` varchar(64) NOT NULL,
  `test_id` varchar(64) NOT NULL,
  `started_at` datetime(6) NOT NULL,
  `duration_minutes` int NOT NULL,
  `status` varchar(20) NOT NULL,
  `final_score` double DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `section_time_spent_json` json DEFAULT NULL,
  `question_order_json` json DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE `outbox_events` (
  `id` varchar(36) NOT NULL,
  `event_type` varchar(100) NOT NULL,
  `aggregate_id` varchar(36) NOT NULL,
  `payload` json NOT NULL,
  `created_at` timestamp NOT NULL,
  `published_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;
