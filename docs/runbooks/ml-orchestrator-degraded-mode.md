# ML Integration Degraded-Mode Runbook

## Scope

Operational runbook for Java-first ML integrations (`src/main/java/com/nonononoki/alovoa/service/ml`) and media-service model adapters (`services/media-service`).

## Fast Checks

1. Java app health:
   - `curl -fsS http://localhost:8080/actuator/health`
2. Media service health (optional legacy profile):
   - `curl -fsS http://localhost:8001/health`
3. Java ML integration status:
   - `curl -fsS http://localhost:8080/api/v1/ml/integrations/status`
4. Recent logs:
   - `docker logs --tail=200 aura-e2e-app`
   - `docker logs --tail=200 aura-e2e-media` (if `python-ml` profile enabled)

## Degraded Modes

### 1) Media adapter failure (attractiveness/moderation/face-quality)

Symptoms:
- media-service adapter command exits non-zero or times out
- endpoints still return baseline/fallback responses

Immediate action:
1. keep services up; fallback behavior is intentional
2. disable unstable adapters:
   - `NSFW_USE_CLIP_NSFW=false`
   - `FACE_QUALITY_USE_ADAPTER=false`
   - `ANTISPOOF_EXTERNAL_ENABLED=false`
3. restart media-service

Validation:
- `POST /moderation/image` still returns valid decision
- `POST /quality/face` still returns score/confidence
- `POST /attractiveness/score` still returns score/confidence

### 2) Unleash/OpenFGA outage

Symptoms:
- `/api/v1/ml/integrations/status` shows `unleash.ok=false` or `openfga.ok=false`
- attractiveness sync policy checks no longer reliable

Immediate action:
1. disable Java-side policy enforcement:
   - `APP_AURA_ATTRACTIVENESS_USE_UNLEASH_FLAG=false`
   - `APP_AURA_ATTRACTIVENESS_OPENFGA_ENFORCE=false`
2. keep scoring sync active

Validation:
- attractiveness sync proceeds without external policy dependency

### 3) Qdrant unavailable

Symptoms:
- integration status reports `qdrant.ok=false`
- no attractiveness hinting/candidate enrichment effects

Immediate action:
1. disable qdrant-dependent controls:
   - `APP_AURA_ML_QDRANT_ENABLED=false`
   - `APP_AURA_ML_QDRANT_ATTRACTIVENESS_HINT_ENABLED=false`
2. keep base scoring/ranking active

Validation:
- reranker and attractiveness sync continue without hint deltas

## Reranker Debug / Triage

### Latest candidate trace for viewer

- `GET /api/v1/matching/reranker/trace/{candidateUuid}`

### Full traces for a request id

- `GET /api/v1/matching/reranker/request/{requestId}?limit=50`

If missing:
1. verify migration `V19__reranker_score_trace_events.sql` is applied
2. verify impression ingestion path is active
3. inspect app logs for table-missing auto-disable warning

## Offline Job Failure

Symptoms:
- Java job runner exits non-zero
- no refreshed artifacts in `configs/` or `logs/`

Checks:
1. Python dependencies:
   - `python3 -m pip install -r scripts/recommender/requirements.txt`
2. DB reachability/config:
   - verify relevant env vars (e.g., `CF_DB_URL`, `ROLLOUT_DB_URL`)
3. runner command:
   - `java -cp target/classes com.nonononoki.alovoa.jobs.ml.MlJobsRunner <job>`

Mitigation:
1. run single script directly to isolate environment issues
2. keep optional features disabled until artifact refresh succeeds

## Smoke Verification

Run service-level smoke checks:

```bash
./scripts/smoke/smoke_orchestrator_modes.sh all
```

Expected:
- integration status endpoint reachable
- fallback behavior preserved when adapters are disabled
- candidate enrichment endpoint validates malformed requests with HTTP 400
