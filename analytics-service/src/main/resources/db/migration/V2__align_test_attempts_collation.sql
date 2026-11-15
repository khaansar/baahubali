-- test_attempts was created with utf8mb4_0900_ai_ci while test_reports uses
-- utf8mb4_unicode_ci, so the leaderboard join on their id columns fails with
-- "Illegal mix of collations". TiDB cannot change the collation of indexed
-- columns in place, so the table is rebuilt with the matching collation.
CREATE TABLE `test_attempts_unicode` (
  `attempt_id` varchar(36) NOT NULL,
  `test_id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL,
  `started_at` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`attempt_id`),
  KEY `idx_test_attempts_user_first` (`test_id`,`user_id`,`started_at`,`attempt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `test_attempts_unicode` (`attempt_id`, `test_id`, `user_id`, `started_at`, `created_at`)
SELECT `attempt_id`, `test_id`, `user_id`, `started_at`, `created_at` FROM `test_attempts`;

RENAME TABLE `test_attempts` TO `test_attempts_old`, `test_attempts_unicode` TO `test_attempts`;

DROP TABLE `test_attempts_old`;
