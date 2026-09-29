CREATE TABLE test_reports (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    attempt_id VARCHAR(36) NOT NULL UNIQUE,
    user_id VARCHAR(36) NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    test_series_id VARCHAR(36) NOT NULL,
    test_id VARCHAR(36) NOT NULL,

    total_score DECIMAL(6, 2) NOT NULL DEFAULT 0.00,
    max_score DECIMAL(6, 2) NOT NULL DEFAULT 0.00,
    accuracy_percentage DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    time_taken_seconds INT NOT NULL DEFAULT 0,
    `rank` INT DEFAULT NULL,
    percentile DECIMAL(5, 2) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING', -- PROCESSING | COMPLETED | FAILED

    report_data JSON DEFAULT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_test (user_id, test_id),
    INDEX idx_user_series (user_id, test_series_id),
    INDEX idx_user_category (user_id, category_id),
    INDEX idx_test_status (test_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE section_performance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    test_id VARCHAR(36) NOT NULL,
    section_id VARCHAR(36) NOT NULL,
    section_name VARCHAR(100) NOT NULL,

    total_questions INT NOT NULL DEFAULT 0,
    correct_count INT NOT NULL DEFAULT 0,
    incorrect_count INT NOT NULL DEFAULT 0,
    unattempted_count INT NOT NULL DEFAULT 0,
    accuracy_percentage DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    time_spent_seconds INT NOT NULL DEFAULT 0,

    topic_name VARCHAR(100) DEFAULT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_attempt (attempt_id),
    INDEX idx_user_section (user_id, section_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE test_leaderboard_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_id VARCHAR(36) NOT NULL,
    attempt_id VARCHAR(36) NOT NULL UNIQUE,
    user_id VARCHAR(36) NOT NULL,
    score DECIMAL(6, 2) NOT NULL,
    time_taken_seconds INT NOT NULL,
    `rank` INT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_leaderboard_rank (test_id, score DESC, time_taken_seconds ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
