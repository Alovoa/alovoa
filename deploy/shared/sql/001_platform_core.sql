-- Shared platform schema for integrated and standalone donation apps.
-- Works for both Neon Postgres (edge) and VM-hosted Postgres setups.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS platform_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_user_id TEXT NOT NULL UNIQUE,
    email TEXT,
    display_name TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS platform_apps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_slug TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    mode TEXT NOT NULL CHECK (mode IN ('INTEGRATED', 'STANDALONE')),
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PAUSED', 'ARCHIVED')),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS platform_sponsorships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sponsor_user_id TEXT NOT NULL,
    beneficiary_user_id TEXT NOT NULL,
    app_slug TEXT NOT NULL REFERENCES platform_apps (app_slug),
    tier TEXT NOT NULL DEFAULT 'DONATION',
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'EXPIRED', 'REVOKED')),
    starts_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    approved_by TEXT,
    approved_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS platform_sponsor_passes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_slug TEXT NOT NULL REFERENCES platform_apps (app_slug),
    sponsor_user_id TEXT NOT NULL,
    pass_code TEXT NOT NULL UNIQUE,
    beneficiary_user_id TEXT,
    status TEXT NOT NULL DEFAULT 'ISSUED' CHECK (status IN ('ISSUED', 'REDEEMED', 'EXPIRED', 'REVOKED')),
    expires_at TIMESTAMPTZ NOT NULL,
    redeemed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pass_redemption_consistency CHECK (
        (status = 'REDEEMED' AND beneficiary_user_id IS NOT NULL)
        OR status <> 'REDEEMED'
    )
);

CREATE TABLE IF NOT EXISTS platform_entitlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id TEXT NOT NULL,
    app_slug TEXT NOT NULL REFERENCES platform_apps (app_slug),
    tier TEXT NOT NULL DEFAULT 'DONATION',
    source_type TEXT NOT NULL CHECK (source_type IN ('SPONSORSHIP', 'SPONSOR_PASS', 'MANUAL')),
    source_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED')),
    starts_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, app_slug, source_type, source_id)
);

CREATE INDEX IF NOT EXISTS idx_platform_apps_status ON platform_apps (status);
CREATE INDEX IF NOT EXISTS idx_platform_sponsorships_app_status ON platform_sponsorships (app_slug, status);
CREATE INDEX IF NOT EXISTS idx_platform_sponsor_passes_lookup ON platform_sponsor_passes (pass_code, status);
CREATE INDEX IF NOT EXISTS idx_platform_entitlements_user_app ON platform_entitlements (user_id, app_slug, status, expires_at);

CREATE OR REPLACE VIEW platform_active_entitlements AS
SELECT
    e.user_id,
    e.app_slug,
    e.tier,
    e.source_type,
    e.source_id,
    e.starts_at,
    e.expires_at
FROM platform_entitlements e
WHERE e.status = 'ACTIVE'
  AND (e.expires_at IS NULL OR e.expires_at > NOW());

INSERT INTO platform_apps (app_slug, display_name, mode, status)
VALUES ('aura', 'AURA', 'INTEGRATED', 'ACTIVE')
ON CONFLICT (app_slug) DO NOTHING;
