ALTER TABLE user_image
    ADD COLUMN IF NOT EXISTS source_provider VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS source_url VARCHAR(1024) NULL,
    ADD COLUMN IF NOT EXISTS source_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_user_image_source_provider ON user_image(source_provider);
