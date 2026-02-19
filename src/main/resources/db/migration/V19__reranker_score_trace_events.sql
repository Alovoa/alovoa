-- V19: Persist reranker score traces for debugging and offline analysis.

CREATE TABLE IF NOT EXISTS reranker_score_trace_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ts DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    request_id VARCHAR(64),
    viewer_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    segment_key VARCHAR(255) NOT NULL,
    variant VARCHAR(32) NOT NULL DEFAULT 'control',

    s DOUBLE,
    f_exposure DOUBLE,
    f_capacity DOUBLE,
    f_gap DOUBLE,
    f_collaborative DOUBLE,
    ucb DOUBLE,
    final_score DOUBLE,
    desirability_decile TINYINT,
    window_stats_json TEXT,

    CONSTRAINT fk_reranker_trace_viewer FOREIGN KEY (viewer_id) REFERENCES user(id) ON DELETE CASCADE,
    CONSTRAINT fk_reranker_trace_candidate FOREIGN KEY (candidate_id) REFERENCES user(id) ON DELETE CASCADE,

    INDEX idx_reranker_trace_ts (ts),
    INDEX idx_reranker_trace_request (request_id),
    INDEX idx_reranker_trace_viewer_candidate_ts (viewer_id, candidate_id, ts),
    INDEX idx_reranker_trace_segment_variant_ts (segment_key, variant, ts)
);
