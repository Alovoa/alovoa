-- V17: Hidden visual-attractiveness priors sourced from OSS media pipeline.
-- This data is backend-only and used by ranking blend; never exposed in UI payloads.

CREATE TABLE IF NOT EXISTS user_visual_attractiveness (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    visual_score DOUBLE NOT NULL DEFAULT 0.5,
    confidence DOUBLE NOT NULL DEFAULT 0.0,
    source_provider VARCHAR(80) NOT NULL DEFAULT 'deepface+mediapipe',
    model_version VARCHAR(80) NOT NULL DEFAULT 'oss_v1',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_visual_attractiveness_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_visual_attractiveness_user (user_id),
    INDEX idx_visual_attractiveness_updated (updated_at),
    INDEX idx_visual_attractiveness_provider (source_provider, model_version)
);
