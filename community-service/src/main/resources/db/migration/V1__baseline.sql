-- Baseline schema for community-service.
-- Squashed from all earlier migrations; equivalent to running them in order on an empty database.

CREATE TABLE `faqs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `target_id` varchar(255) DEFAULT NULL,
  `question` text NOT NULL,
  `answer` text NOT NULL,
  `display_order` int DEFAULT NULL,
  `created_at` timestamp(6) NULL DEFAULT NULL,
  `updated_at` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_faqs_target_order` (`target_id`,`display_order`)
) ENGINE=InnoDB;

CREATE TABLE `reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(255) NOT NULL,
  `target_id` varchar(255) NOT NULL,
  `target_type` varchar(50) NOT NULL,
  `rating` int NOT NULL,
  `comment` text,
  `status` varchar(50) NOT NULL DEFAULT 'PENDING',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp(6) NULL DEFAULT NULL,
  `moderation_reason` text,
  `moderated_by` varchar(255) DEFAULT NULL,
  `moderated_at` timestamp(6) NULL DEFAULT NULL,
  `show_on_homepage` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target_review` (`user_id`,`target_id`,`target_type`),
  KEY `idx_reviews_target_status` (`target_id`,`status`),
  KEY `idx_reviews_status_created_at` (`status`,`created_at`),
  KEY `idx_reviews_show_on_homepage` (`show_on_homepage`),
  CONSTRAINT `reviews_chk_1` CHECK (((`rating` >= 1) and (`rating` <= 5)))
) ENGINE=InnoDB;
