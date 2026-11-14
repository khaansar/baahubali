-- Drops data nothing reads: the write-only leaderboard snapshot table, the report rank
-- (removed from the report) and the never-written section topic name.
DROP TABLE IF EXISTS test_leaderboard_snapshots;

ALTER TABLE test_reports DROP COLUMN `rank`;

ALTER TABLE section_performance DROP COLUMN topic_name;
