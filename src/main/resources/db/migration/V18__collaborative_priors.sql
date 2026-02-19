-- V18: Optional collaborative-filtering priors for reranker (implicit/lightfm offline jobs).
-- Keeps current matching pipeline intact; used only when feature-flagged in reranker config.

CREATE TABLE IF NOT EXISTS user_collaborative_prior (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    segment_key VARCHAR(255) NOT NULL DEFAULT '*',
    prior_score DOUBLE NOT NULL DEFAULT 0.5,
    confidence DOUBLE NOT NULL DEFAULT 0.0,
    source_model VARCHAR(80) NOT NULL DEFAULT 'implicit_als',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_collaborative_prior_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_collaborative_prior_user_segment (user_id, segment_key),
    INDEX idx_collaborative_prior_segment_score (segment_key, prior_score),
    INDEX idx_collaborative_prior_updated (updated_at)
);

