# OSS Integration Plan (Java-First Runtime)

Date: 2026-02-19

## Objective

Integrate the selected OSS capability set into the production runtime with:
- Java backend as orchestration/control plane
- Python media/adapters for model execution
- Feature flags + safe fallback behavior
- Persisted analytics signals and offline calibration/evaluation jobs

## Scope

1. `unitaryai/detoxify`
2. `LSIbabnikz/FaceQAN`
3. `LAION-AI/CLIP-based-NSFW-Detector`
4. `SCLBD/DeepfakeBench`
5. `recommenders-team/recommenders`

## Completion Tracker

Status legend:
- `[x]` complete
- `[~]` in progress
- `[ ]` not started

| Stream | Weight | Current | Weighted |
|---|---:|---:|---:|
| Runtime endpoint integration (media-service + Java callers) | 35% | 100% | 35.0% |
| Java backend persistence + policy wiring | 25% | 100% | 25.0% |
| Offline jobs/calibration/eval integration | 20% | 100% | 20.0% |
| Config/docs/deploy wiring | 10% | 100% | 10.0% |
| Tests/build verification | 10% | 100% | 10.0% |
| **Total** | **100%** |  | **100.0%** |

## Repo-by-Repo Integration Matrix

| Repo | Integration Type | Runtime/Job Path | Status |
|---|---|---|---|
| `detoxify` | Online text moderation | `services/media-service/main.py` (`POST /moderation/text`), `services/media-service/scripts/text_detoxify_adapter.py`, Java call-chain in `src/main/java/com/nonononoki/alovoa/service/ContentModerationService.java` | `[x]` |
| `FaceQAN` | Face quality signal + quality gating | `services/media-service/main.py` (`POST /quality/face` + adapter blending), `services/media-service/scripts/faceqan_adapter.py`, persisted by `src/main/java/com/nonononoki/alovoa/matching/rerank/service/FaceQualityScoringService.java`, applied in `src/main/java/com/nonononoki/alovoa/matching/rerank/service/VisualAttractivenessSyncService.java` | `[x]` |
| `CLIP-based-NSFW-Detector` | NSFW ensemble provider | `services/media-service/main.py` tri-ensemble (OpenNSFW2 + NudeNet + CLIP), `services/media-service/scripts/nsfw_clip_adapter.py` | `[x]` |
| `DeepfakeBench` | Offline threshold calibration (Deepfake) | `scripts/recommender/build_deepfake_calibration.py`, Java job runner `src/main/java/com/nonononoki/alovoa/jobs/ml/MlJobsRunner.java` (`build-deepfake-calibration`) | `[x]` |
| `recommenders` | Offline reranker evaluation patterns | `scripts/recommender/evaluate_reranker_offline.py`, Java job runner `MlJobsRunner` (`evaluate-reranker-offline`) | `[x]` |

## Runtime Wiring Summary

- Media-service endpoints:
  - `POST /attractiveness/score`
  - `POST /moderation/image`
  - `POST /moderation/text`
  - `POST /quality/face`
- Java attractiveness sync now includes:
  - Face-quality assessment + event persistence (`face_quality_event`)
  - Quality gate and quality-weighted blending
  - Optional Unleash gate
  - Optional OpenFGA entitlement enforcement
  - Optional bounded Qdrant attractiveness hints
- Java ML integrations:
  - `QdrantIntegrationService`
  - `UnleashIntegrationService`
  - `OpenFgaIntegrationService`
  - status endpoints in `MlIntegrationController`

## Data Model

- `V20__moderation_signal_expansion.sql`
- `V21__face_quality_events.sql`
- `V18__collaborative_priors.sql`
- `V19__reranker_score_trace_events.sql`

## Validation Targets

- Python media-service tests:
  - `services/media-service/test_main.py`
- Java compile/tests:
  - `mvn -DskipTests package`
  - targeted moderation/reranker tests

## Source of Truth

- `/Users/tkhan/IdeaProjects/alovoa/third_party/README.md`
- `/Users/tkhan/IdeaProjects/alovoa/docs/oss-ts-implementation-plan.md`
