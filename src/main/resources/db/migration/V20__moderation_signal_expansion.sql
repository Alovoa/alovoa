ALTER TABLE content_moderation_event
    ADD COLUMN IF NOT EXISTS provider VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS model_version VARCHAR(120) NULL,
    ADD COLUMN IF NOT EXISTS source_mode VARCHAR(32) NULL,
    ADD COLUMN IF NOT EXISTS signal_json MEDIUMTEXT NULL;

CREATE INDEX IF NOT EXISTS idx_moderation_provider ON content_moderation_event(provider);
CREATE INDEX IF NOT EXISTS idx_moderation_source_mode ON content_moderation_event(source_mode);
