-- Baseline schema for iam-service.
-- Squashed from all earlier migrations; equivalent to running them in order on an empty database.

CREATE TABLE `audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` binary(36) NOT NULL,
  `actor_id` binary(36) DEFAULT NULL,
  `actor_name` varchar(205) DEFAULT NULL,
  `actor_role` varchar(32) DEFAULT NULL,
  `action` varchar(100) NOT NULL,
  `resource_type` varchar(64) NOT NULL,
  `resource_id` varchar(128) DEFAULT NULL,
  `service` varchar(64) NOT NULL,
  `endpoint` varchar(255) DEFAULT NULL,
  `http_method` varchar(16) DEFAULT NULL,
  `status_code` int DEFAULT NULL,
  `request_id` varchar(100) DEFAULT NULL,
  `metadata` json DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_audit_event_id` (`event_id`),
  KEY `idx_audit_actor_time` (`actor_id`,`created_at`),
  KEY `idx_audit_action_time` (`action`,`created_at`),
  KEY `idx_audit_resource_time` (`resource_type`,`resource_id`,`created_at`),
  KEY `idx_audit_service_time` (`service`,`created_at`),
  KEY `idx_audit_created_at` (`created_at`)
) ENGINE=InnoDB;

CREATE TABLE `processed_attempt_events` (
  `event_id` varchar(36) NOT NULL,
  `processed_at` timestamp NOT NULL,
  PRIMARY KEY (`event_id`)
) ENGINE=InnoDB;

CREATE TABLE `user_daily_activity` (
  `id` binary(16) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  `activity_date` date NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`,`activity_date`)
) ENGINE=InnoDB;

CREATE TABLE `users` (
  `id` varchar(36) NOT NULL,
  `first_name` varchar(100) NOT NULL,
  `last_name` varchar(100) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `role` enum('STUDENT','EVALUATOR','ADMIN') NOT NULL,
  `avatar_url` varchar(512) DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL DEFAULT NULL,
  `tests_attempted_count` int NOT NULL DEFAULT '0',
  `phone` varchar(20) DEFAULT NULL,
  `target_exam` varchar(100) DEFAULT NULL,
  `email_verified` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_users_first_name` (`first_name`),
  KEY `idx_users_last_name` (`last_name`)
) ENGINE=InnoDB;

CREATE TABLE `user_identities` (
  `id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `provider` varchar(32) NOT NULL,
  `provider_subject` varchar(255) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_subject` (`provider`,`provider_subject`),
  UNIQUE KEY `uk_user_provider` (`user_id`,`provider`),
  CONSTRAINT `fk_user_identity_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;
