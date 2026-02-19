# Deployment Strategy (Dual Track)

This repository now supports two production deployment tracks:

| Track | Best for | Cost floor (typical) | Repo path |
|---|---|---|---|
| Edge + Serverless Postgres | Early stage, bursty traffic, many small apps | Near $0 to low single digits while usage is small | `deploy/edge/platform-worker` |
| Single VM + Docker | Always-on backend, straightforward ops | Low fixed monthly VM bill | `deploy/vm` |

## Shared platform model

Both tracks use the same core domain model for donation-backed access:

- `platform_users`
- `platform_apps`
- `platform_sponsorships`
- `platform_sponsor_passes`
- `platform_entitlements`

Schema file:

- `deploy/shared/sql/001_platform_core.sql`

This supports:

- integrated apps (shared auth/data) via `app_slug`
- standalone apps (separate runtime, common entitlement backbone)
- sponsor-pays-beneficiary flows with either pass codes or manual approval

## Choose a starting point

### Option A: Edge + Neon

```bash
cd deploy/edge/platform-worker
npm install
npm run dev
```

Then deploy with Wrangler and point your static frontends to this API.

### Option B: Single VM

```bash
cd deploy/vm
cp .env.example .env
./deploy.sh
```

Then map your DNS to the VM and let Caddy issue TLS certificates.
