CREATE TABLE IF NOT EXISTS face_quality_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    content_type VARCHAR(40) NULL,
    quality_score DOUBLE,
    confidence DOUBLE,
    provider VARCHAR(100),
    model_version VARCHAR(120),
    signal_json MEDIUMTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_face_quality_user (user_id),
    INDEX idx_face_quality_content_type (content_type),
    INDEX idx_face_quality_created (created_at)
);
