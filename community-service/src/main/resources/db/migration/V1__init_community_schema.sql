CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'APPROVED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE faqs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_id VARCHAR(255),
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    display_order INT
);

CREATE INDEX idx_reviews_target_status ON reviews(target_id, status);
CREATE INDEX idx_faqs_target_order ON faqs(target_id, display_order);