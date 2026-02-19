# OSS Porting Acceptance Report (V2 Extension)

Date: 2026-02-19

## Objective

Confirm the V2 OSS-to-TS extension is complete for:

1. `unitaryai/detoxify`
2. `LSIbabnikz/FaceQAN`
3. `LAION-AI/CLIP-based-NSFW-Detector`
4. `SCLBD/DeepfakeBench`
5. `recommenders-team/recommenders`

## Acceptance Checklist

### 1) TS contracts, endpoints, and fallbacks

- [x] Added TS contracts for text moderation and face quality.
- [x] Added endpoints:
  - `POST /moderation/text`
  - `POST /quality/face`
- [x] All new providers are config/flag driven (no hardcoded mandatory path).
- [x] Safe fallbacks exist for local provider failures.

### 2) Online feature integrations

- [x] Detoxify integrated via adapter + local provider:
  - `services/media-service/scripts/text_detoxify_adapter.py`
  - `services/ml-orchestrator-ts/src/providers/localTextModeration.ts`
- [x] FaceQAN-quality integrated via adapter + local provider:
  - `services/media-service/scripts/faceqan_adapter.py`
  - `services/ml-orchestrator-ts/src/providers/localFaceQuality.ts`
- [x] CLIP NSFW integrated as moderation ensemble provider:
  - `services/media-service/scripts/nsfw_clip_adapter.py`
  - `services/ml-orchestrator-ts/src/providers/localModeration.ts`
- [x] Quality signal wired into attractiveness/moderation confidence path.

### 3) Offline feature integrations

- [x] Deepfake calibration script + TS job wrapper:
  - `scripts/recommender/build_deepfake_calibration.py`
  - `services/ml-orchestrator-ts/src/jobs/buildDeepfakeCalibration.ts`
- [x] Offline reranker evaluation script + TS job wrapper:
  - `scripts/recommender/evaluate_reranker_offline.py`
  - `services/ml-orchestrator-ts/src/jobs/evaluateRerankerOffline.ts`

### 4) Backend integration + schema

- [x] Java moderation service can call text moderation endpoint with fallback chain.
- [x] Moderation event metadata fields added.
- [x] Migrations added:
  - `V20__moderation_signal_expansion.sql`
  - `V21__face_quality_events.sql`

### 5) Validation run results

- [x] TypeScript typecheck passed.
- [x] TypeScript tests passed.
- [x] TS build passed.
- [x] Python compile checks passed.
- [x] Python media-service tests passed.
- [x] Java backend package build passed with Java 17 (`mvn -DskipTests package`).

## Notes

- DeepfakeBench remains offline-only in this extension (calibration/evaluation path).
- Runtime behavior for all new modules remains behind feature flags/config and preserves fallback behavior.
