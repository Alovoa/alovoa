# Edge Deployment (Cloudflare Workers + Neon)

This module provides a low-cost shared platform API for:

- app registry (`platform_apps`)
- sponsorship records (`platform_sponsorships`)
- honor/manual sponsor passes (`platform_sponsor_passes`)
- user entitlements (`platform_entitlements`)

It is designed for a multi-app donation platform where each app can be:

- `INTEGRATED`: shared auth/data model with `app_slug` partitioning
- `STANDALONE`: same entitlement backbone, separate app runtime

## 1) Provision database schema

From the repo root:

```bash
psql "$DATABASE_URL" -f deploy/shared/sql/001_platform_core.sql
```

## 2) Configure worker

```bash
cd deploy/edge/platform-worker
cp .dev.vars.example .dev.vars
npm install
```

Set secrets for production:

```bash
npx wrangler secret put DATABASE_URL
npx wrangler secret put PLATFORM_ADMIN_TOKEN
```

## 3) Run locally

```bash
npm run dev
```

## 4) Deploy

```bash
npm run deploy
```

## API surface

- `GET /health`
- `GET /v1/apps`
- `POST /v1/apps` (admin)
- `POST /v1/sponsorships` (admin)
- `POST /v1/sponsorships/:id/approve` (admin)
- `POST /v1/sponsor-passes` (admin)
- `POST /v1/sponsor-passes/redeem`
- `GET /v1/users/:userId/entitlements`
- `POST /v1/entitlements/reconcile` (admin)

Admin routes require:

```http
Authorization: Bearer <PLATFORM_ADMIN_TOKEN>
```

## Example requests

Create or upsert an app:

```bash
curl -X POST "$EDGE_URL/v1/apps" \
  -H "Authorization: Bearer $PLATFORM_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "appSlug": "habit-lab",
    "displayName": "Habit Lab",
    "mode": "STANDALONE",
    "status": "ACTIVE"
  }'
```

Issue sponsor pass:

```bash
curl -X POST "$EDGE_URL/v1/sponsor-passes" \
  -H "Authorization: Bearer $PLATFORM_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "appSlug": "aura",
    "sponsorUserId": "user_123",
    "expiresInDays": 30
  }'
```

Redeem sponsor pass:

```bash
curl -X POST "$EDGE_URL/v1/sponsor-passes/redeem" \
  -H "Content-Type: application/json" \
  -d '{
    "passCode": "AB12CD34EF56",
    "beneficiaryUserId": "user_456"
  }'
```
