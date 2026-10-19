ALTER TABLE mock_tests
    ADD COLUMN access_code VARCHAR(64) NULL,
    ADD COLUMN window_start_time TIMESTAMP NULL,
    ADD COLUMN window_end_time TIMESTAMP NULL;
