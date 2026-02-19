# Reciprocal Reranker + Hidden Backend Attractiveness Score

## Goal

Add a modular reranker on top of the existing candidate generator to reduce winner-take-most exposure concentration while preserving conversation conversion.

## Non-breaking integration

Current pipeline remains source-of-truth for candidate generation:

- Existing: candidate generation and base compatibility (`S_ij`)
- Added: `MatchRerankerService` layer (feature-flagged, segment-aware)

Flow per feed request:

1. existing matching pipeline returns candidate DTOs
2. reranker resolves feature flag/config for the viewer segment
3. control bucket: original ordering preserved
4. treatment bucket: compute `R_ij`, sort by rerank score
5. log impression events with request metadata and traceable factors

## Segment key

At minimum:

- `gender`
- `seeking_gender`
- `age_bucket`
- `geo_bucket`

Encoded as:

`gender:<g>|seeking:<s>|age:<a>|geo:<geo>`

## Formulas implemented

Base score normalized to `[0,1]` from existing compatibility percentage:

- `S_ij = clamp(compatibilityScore / 100)`

Exposure balancing:

- `f_exposure(j) = 1 / (1 + E_j / tau)^p`

Capacity throttling:

- `C_j = open_matches + unread_threads + pending_inbound_likes`
- `f_capacity(j) = 1 / (1 + C_j / kappa)`

Desirability gap:

- `A(u) = inbound_likes_7d / impressions_7d`
- `D(u) = percentile(A(u))` within segment
- smoothing: `p_smoothed = w*p + (1-w)*p_baseline`, `w = n/(n+n0)`
- `f_gap(i,j) = exp(-lambda * abs(D(i) - D(j)))`

Exploration (UCB-like):

- based on underexposed desirability bucket for viewer + candidate underexposure
- scaled by `epsilon`

Final rerank score:

- `R_ij = S_ij * f_exposure * f_capacity * f_gap * f_collab + epsilon * UCB_ij`

Collaborative prior (optional, feature-flagged):

- offline models (`implicit` / `lightfm`) compute per-user prior and confidence
- persisted in `user_collaborative_prior`
- default policy maps compatibility/alignment into bounded factor:
  - `f_collab in [collaborativeMinFactor, collaborativeMaxFactor]`
  - disabled by default (`enableCollaborativePrior=false`)

Hard quality floor:

- drop candidates with `S_ij < s_min`

## Hidden backend attractiveness score

A hidden score is maintained in `user_rolling_stats.backend_attractiveness_score`.

It is **not exposed in UI APIs** and is used only in ranking math.

Definition:

- behavioral prior:
  - compute percentile `D_percentile_7d` within segment from `A_7d`
  - apply cold-start smoothing:
    - `behavioral_prior = (impressions_7d * D + n0 * baseline) / (impressions_7d + n0)`
- visual prior (optional, backend-only):
  - sourced from OSS media pipeline (`deepface` + optional `insightface` + optional `mediapipe`)
  - optional external scorer adapter can call repository-specific models (ComboLoss/BeautyPredict/FaceAttract/MetaFBP/3D models) via command hook
  - persisted in `user_visual_attractiveness`
  - confidence-weighted blend into backend score:
    - `backend_attractiveness_score = behavioral_prior * (1 - visual_weight * confidence) + visual_score * (visual_weight * confidence)`

This keeps market behavior as the primary signal while allowing controlled visual prior influence.

## Policy plugin interfaces

Swappable interfaces:

- `ExposurePolicy`
- `CapacityPolicy`
- `DesirabilityPolicy`
- `ExplorationPolicy`
- `CollaborativePriorPolicy`

Default implementations use the formulas above.

## Feature flags and safe fallback

Flag table: `feature_flags`

- `flag_name = MATCH_RERANKER`
- `segment_key` specific or `*`
- `enabled`
- `json_config` (tau, p, kappa, lambda, sMin, epsilon, n0, trafficPercent)
- optional collaborative params:
  - `enableCollaborativePrior`
  - `collaborativeBeta`
  - `collaborativeMinFactor`
  - `collaborativeMaxFactor`

Safety rules implemented:

- flag off or control bucket => keep existing ordering
- any policy/runtime error => fallback to existing ordering
- missing stats => neutral factors (`f_exposure=1`, `f_capacity=1`, baseline desirability)

## Data model added

- `impression_events`
- `like_events`
- `match_events`
- `message_events`
- `user_rolling_stats`
- `user_visual_attractiveness`
- `feature_flags`
- `user_collaborative_prior`

## Jobs

`RollingStatsAggregationService`:

- hourly + nightly aggregation (configurable cron)
- updates `user_rolling_stats`
- recomputes segment percentiles + hidden attractiveness score

`VisualAttractivenessSyncService`:

- scheduled sync of profile pictures to media-service `/attractiveness/score`
- persists hidden visual priors in `user_visual_attractiveness`
- uses stale-window refresh + safe fallback on service failures

Offline collaborative prior job:

- `scripts/recommender/train_cf_priors.py`
- computes user priors from implicit feedback logs using `implicit` (ALS) or `lightfm`
- outputs SQL upserts for `user_collaborative_prior`

### External scorer hook

Media-service supports an optional external scorer command for repo-specific models.

Config:

- `ATTRACTIVENESS_EXTERNAL_SCORER_ENABLED=true`
- `ATTRACTIVENESS_EXTERNAL_SCORER_CMD='python /path/to/scorer.py --image {image_path} --view {view}'`
- `ATTRACTIVENESS_EXTERNAL_WEIGHT=0.25`
- `ATTRACTIVENESS_EXTERNAL_TIMEOUT_SEC=10`

Expected stdout JSON from external scorer:

```json
{
  "score": 0.64,
  "confidence": 0.73,
  "signals": {
    "model_quality": 0.71
  }
}
```

This enables plugging in ComboLoss / BeautyPredict / FaceAttract / MetaFBP / 3D model pipelines without changing the serving API contract.

Additional OSS media hooks:

- anti-spoof command hook:
  - `ANTISPOOF_EXTERNAL_ENABLED=true`
  - `ANTISPOOF_EXTERNAL_CMD='python /path/to/silent_face_antispoof_adapter.py --image {image_path}'`
- image moderation command hook:
  - `NSFW_EXTERNAL_SCORER_ENABLED=true`
  - `NSFW_EXTERNAL_SCORER_CMD='python /path/to/nsfw_nudenet_adapter.py --image {image_path}'`

Backfill script:

- `scripts/sql/backfill_reranker_events.sql`

Score-trace persistence:

- migration `V19__reranker_score_trace_events.sql`
- populated during impression ingestion
- debug endpoints:
  - `GET /api/v1/matching/reranker/trace/{matchUuid}`
  - `GET /api/v1/matching/reranker/request/{requestId}?limit=50`

## Observability

Per-candidate trace (`ScoreTrace`) includes:

- `S`
- `f_exposure`
- `f_capacity`
- `f_gap`
- `ucb`
- `finalScore`
- `segment`
- `desirabilityDecile`
- window stats fields

## A/B plan

Control:

- existing ordering only

Treatment:

- reranked `R_ij`

Ramp:

1. set `enabled=true`, `trafficPercent=1`
2. move to `trafficPercent=10`
3. move to `trafficPercent=50`

Guardrails:

- conversations per impression does not drop more than threshold
- block/report rate does not increase above threshold
- median time-to-first-conversation improves for median segments

Rollback:

- set `enabled=false` or `trafficPercent=0` in `feature_flags`

## Load/perf notes

- candidate stats fetched in batch (`IN (...)`) by segment
- O(n log n) sort over rerank candidate set
- impression writes buffered and batch-inserted with queue fallback
- rolling stats precomputed offline for fast online reads
