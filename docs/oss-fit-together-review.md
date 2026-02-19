# OSS Fit-Together Review (Detoxify + FaceQAN + CLIP NSFW + DeepfakeBench + Recommenders)

Date: 2026-02-19

## Scope

Repos reviewed:

1. `unitaryai/detoxify`
2. `LSIbabnikz/FaceQAN`
3. `LAION-AI/CLIP-based-NSFW-Detector`
4. `SCLBD/DeepfakeBench`
5. `recommenders-team/recommenders`

All five are now cloned under:
- `third_party/repos`

## Fit Summary

### High fit for online integration

1. `unitaryai/detoxify`
- Role: text toxicity scoring for chat and profile text.
- Best fit: Java `ContentModerationService` fallback/ensemble path.
- Why: direct replacement/improvement over keyword-only fallback and optional Perspective API path.

2. `LAION-AI/CLIP-based-NSFW-Detector`
- Role: additional visual safety classifier in moderation ensemble.
- Best fit: `services/media-service/main.py` NSFW path as third provider alongside `opennsfw2` and `NudeNet`.
- Why: low-latency score head on CLIP embeddings, easy to blend by weighted score.

3. `LSIbabnikz/FaceQAN`
- Role: face image quality signal (confidence prior), not attractiveness itself.
- Best fit: media quality gating before attractiveness/liveness scoring.
- Why: quality signal helps suppress low-quality-image false confidence.

### High fit for offline-only integration

4. `SCLBD/DeepfakeBench`
- Role: standardized deepfake detector training/evaluation and calibration.
- Best fit: offline benchmark/calibration pipeline for threshold tuning and detector selection.
- Why: heavy stack, dataset-intensive, training-oriented; not suitable as direct online dependency.
- License note: CC BY-NC 4.0 (non-commercial restrictions). Keep to research/calibration usage unless licensing is cleared.

5. `recommenders-team/recommenders`
- Role: offline evaluation utilities and benchmark patterns (MAP/NDCG/precision/recall/diversity/coverage).
- Best fit: reranker metrics validation and experiment sanity checks.
- Why: complements existing reranker guardrail SQL with additional reproducible offline eval tooling.

## How They Fit Together (Unified Pipeline)

### A. Media trust + quality stack

1. Face quality stage:
- Use `FaceQAN` as `face_quality_score` in `[0,1]`.
- Apply as confidence multiplier for attractiveness/liveness outputs.

2. Safety stage:
- NSFW ensemble = `opennsfw2` + `NudeNet` + `CLIP NSFW`.
- Segment policy overrides continue to apply in TS orchestrator.

3. Authenticity stage:
- Keep online anti-spoof in media-service.
- Use `DeepfakeBench` offline to choose/calibrate model thresholds and monitor drift.

### B. Text trust stack

1. Message/profile moderation:
- `Detoxify` outputs per-label risk (`toxicity`, `threat`, `insult`, `identity_attack`, etc.).
- Blend with keyword/policy checks in `ContentModerationService`.
- Log scores in moderation events for trend/risk analysis.

### C. Matching/reranker evaluation stack

1. Online:
- existing reranker + rollout guardrails stay in place.

2. Offline:
- `recommenders` evaluation utils compute ranking/diversity/coverage metrics over snapshots.
- use outputs to validate concentration-mitigation impact and detect regressions.

## Recommended Implementation Order

1. `detoxify` adapter + Java moderation integration (highest immediate product value).
2. CLIP NSFW adapter + media-service ensemble blending.
3. FaceQAN adapter as face quality prior signal.
4. Recommenders offline evaluation harness for reranker experiments.
5. DeepfakeBench offline calibration workflow (no online dependency).

## Integration Boundaries

### Keep online path light

- Online services should only call thin adapters that return normalized scores.
- Heavy training/evaluation repos remain offline jobs.

### Feature flags + fallback

- Every new provider behind env/flag with hard fallback to current behavior.
- If adapter fails, continue using existing providers and mark signal as unavailable.

## Concrete hook points in this repo

1. Text moderation:
- `src/main/java/com/nonononoki/alovoa/service/ContentModerationService.java`

2. Image/video moderation + liveness:
- `services/media-service/main.py`
- `services/media-service/scripts/`

3. TS policy routing and flags:
- `services/ml-orchestrator-ts/src/providers/orchestratorService.ts`
- `services/ml-orchestrator-ts/src/config.ts`

4. Reranker evaluation/guardrails:
- `scripts/recommender/`
- `docs/sql/reranker_dashboard_queries.sql`

## Risks and Constraints

1. Licensing:
- DeepfakeBench is non-commercial by default (CC BY-NC 4.0).

2. Operational:
- FaceQAN and DeepfakeBench are GPU-heavy and should not be added to synchronous request path without a worker queue.

3. Bias/fairness:
- Detoxify and NSFW models require segment-level threshold calibration and ongoing drift monitoring.

## Bottom Line

Best combined strategy:

1. online safety/quality: `Detoxify + CLIP NSFW + existing NSFW/anti-spoof providers + optional FaceQAN quality prior`
2. offline benchmarking/calibration: `DeepfakeBench + recommenders-team/recommenders`

This gives immediate moderation/ranking gains without destabilizing latency-sensitive paths.

## Implementation Status (2026-02-19)

Status in this repo:

1. `detoxify` integrated (TS + adapter + endpoint + backend fallback wiring).
2. `FaceQAN` integrated as face-quality signal (TS + adapter + confidence wiring).
3. `CLIP-based-NSFW-Detector` integrated into moderation ensemble path.
4. `DeepfakeBench` integrated as offline calibration job (no online dependency).
5. `recommenders-team/recommenders` integrated as offline reranker evaluation workflow.
