-- V16: Reciprocal reranker core data model
-- Adds event logs, rolling stats, and feature-flag config for modular ranking policies.

-- 1) Impression events (feed exposures)
CREATE TABLE IF NOT EXISTS impression_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ts DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    viewer_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    surface VARCHAR(64) NOT NULL DEFAULT 'daily_matches',
    position INT NOT NULL,
    segment_key VARCHAR(255) NOT NULL,
    request_id VARCHAR(64),
    candidate_desirability_decile TINYINT,
    variant VARCHAR(32) NOT NULL DEFAULT 'control',

    CONSTRAINT fk_impression_viewer FOREIGN KEY (viewer_id) REFERENCES user(id) ON DELETE CASCADE,
    CONSTRAINT fk_impression_candidate FOREIGN KEY (candidate_id) REFERENCES user(id) ON DELETE CASCADE,

    INDEX idx_impression_ts (ts),
    INDEX idx_impression_candidate_ts (candidate_id, ts),
    INDEX idx_impression_viewer_ts (viewer_id, ts),
    INDEX idx_impression_segment_ts (segment_key, ts),
    INDEX idx_impression_request (request_id)
);

-- 2) Like events (directional preference events)
CREATE TABLE IF NOT EXISTS like_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ts DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    viewer_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    direction VARCHAR(64) NOT NULL DEFAULT 'viewer_liked_candidate',
    segment_key VARCHAR(255) NOT NULL,

    CONSTRAINT fk_like_viewer FOREIGN KEY (viewer_id) REFERENCES user(id) ON DELETE CASCADE,
    CONSTRAINT fk_like_candidate FOREIGN KEY (candidate_id) REFERENCES user(id) ON DELETE CASCADE,

    INDEX idx_like_ts (ts),
    INDEX idx_like_viewer_ts (viewer_id, ts),
    INDEX idx_like_candidate_ts (candidate_id, ts),
    INDEX idx_like_segment_ts (segment_key, ts)
);

-- 3) Match events (bidirectional positive outcomes)
CREATE TABLE IF NOT EXISTS match_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ts DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_a BIGINT NOT NULL,
    user_b BIGINT NOT NULL,
    segment_key VARCHAR(255) NOT NULL,

    CONSTRAINT fk_match_user_a FOREIGN KEY (user_a) REFERENCES user(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_user_b FOREIGN KEY (user_b) REFERENCES user(id) ON DELETE CASCADE,

    INDEX idx_match_ts (ts),
    INDEX idx_match_user_a_ts (user_a, ts),
    INDEX idx_match_user_b_ts (user_b, ts),
    INDEX idx_match_segment_ts (segment_key, ts)
);

-- 4) Message events (minimal messaging outcomes)
CREATE TABLE IF NOT EXISTS message_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ts DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    conversation_id BIGINT,

    CONSTRAINT fk_message_event_sender FOREIGN KEY (sender_id) REFERENCES user(id) ON DELETE CASCADE,
    CONSTRAINT fk_message_event_receiver FOREIGN KEY (receiver_id) REFERENCES user(id) ON DELETE CASCADE,

    INDEX idx_message_event_ts (ts),
    INDEX idx_message_event_sender_ts (sender_id, ts),
    INDEX idx_message_event_receiver_ts (receiver_id, ts),
    INDEX idx_message_event_conversation_ts (conversation_id, ts)
);

-- 5) Rolling stats table used by online reranker policies
CREATE TABLE IF NOT EXISTS user_rolling_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    segment_key VARCHAR(255) NOT NULL,
    window_start DATETIME NOT NULL,
    window_end DATETIME NOT NULL,

    impressions_7d INT NOT NULL DEFAULT 0,
    inbound_likes_7d INT NOT NULL DEFAULT 0,
    outbound_likes_7d INT NOT NULL DEFAULT 0,
    matches_7d INT NOT NULL DEFAULT 0,

    open_matches INT NOT NULL DEFAULT 0,
    unread_threads INT NOT NULL DEFAULT 0,
    pending_inbound_likes INT NOT NULL DEFAULT 0,

    A_7d DOUBLE NOT NULL DEFAULT 0.0,
    D_percentile_7d DOUBLE NOT NULL DEFAULT 0.5,
    backend_attractiveness_score DOUBLE NOT NULL DEFAULT 0.5,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_rolling_stats_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_rolling_stats_user_segment (user_id, segment_key),
    INDEX idx_rolling_stats_segment (segment_key, D_percentile_7d),
    INDEX idx_rolling_stats_updated (updated_at)
);

-- 6) Feature flags + JSON config
CREATE TABLE IF NOT EXISTS feature_flags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flag_name VARCHAR(120) NOT NULL,
    segment_key VARCHAR(255) NOT NULL DEFAULT '*',
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    json_config TEXT,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_feature_flag (flag_name, segment_key),
    INDEX idx_feature_flag_lookup (flag_name, segment_key, enabled)
);

-- Seed default reranker flag (off by default, safe fallback to S ordering)
INSERT INTO feature_flags (flag_name, segment_key, enabled, json_config)
VALUES (
    'MATCH_RERANKER',
    '*',
    FALSE,
    '{
      "tau": 200,
      "p": 1.0,
      "kappa": 30,
      "lambda": 2.0,
      "sMin": 0.20,
      "epsilon": 0.01,
      "n0": 200,
      "baselineAttractiveness": 0.5,
      "enableExploration": true,
      "enableCollaborativePrior": false,
      "collaborativeBeta": 0.15,
      "collaborativeMinFactor": 0.85,
      "collaborativeMaxFactor": 1.15,
      "debugTrace": false,
      "experimentKey": "reranker_v1",
      "trafficPercent": 100
    }'
)
ON DUPLICATE KEY UPDATE
    json_config = VALUES(json_config);
