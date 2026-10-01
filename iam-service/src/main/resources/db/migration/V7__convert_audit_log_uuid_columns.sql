CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BINARY(36) NOT NULL,
    actor_id BINARY(36) NULL,
    actor_name VARCHAR(205) NULL,
    actor_role VARCHAR(32) NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128) NULL,
    service VARCHAR(64) NOT NULL,
    endpoint VARCHAR(255) NULL,
    http_method VARCHAR(16) NULL,
    status_code INT NULL,
    request_id VARCHAR(100) NULL,
    metadata JSON NULL,
    created_at TIMESTAMP(6) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_audit_event_id UNIQUE (event_id),

    INDEX idx_audit_actor_time (actor_id, created_at),
    INDEX idx_audit_action_time (action, created_at),
    INDEX idx_audit_resource_time (resource_type, resource_id, created_at),
    INDEX idx_audit_service_time (service, created_at),
    INDEX idx_audit_created_at (created_at)
);