# OSS Porting Acceptance Report (V1)

## Objective

Confirm the OSS-to-TS porting program is functionally complete for selected repos and safe to run behind flags in production.

## Acceptance Checklist

### 1) Porting contract

- [x] TS interfaces exist for attractiveness/moderation/orchestration providers.
- [x] Runtime config controls all provider commands and weights.
- [x] Safe fallback paths exist (proxy/baseline/bypass) for failures.
- [x] Unit tests cover core formula and failure behavior.
- [x] Operational docs include enablement + degraded mode actions.

### 2) Repo integration status

- [x] Attractiveness repos integrated through adapters and TS orchestration:
  - ComboLoss, BeautyPredict, FaceAttract, MetaFBP, facial-beauty-prediction
  - deepface, insightface, mediapipe, face-alignment, dlib
  - DECA, Deep3DFaceRecon (geometry priors)
- [x] Safety repos integrated:
  - opennsfw2, NudeNet, Silent-Face-Anti-Spoofing
- [x] Recommender repos integrated:
  - implicit/lightfm offline collaborative prior training
- [x] Infra repos integrated:
  - OpenFGA/Unleash/Qdrant clients with runtime hooks

### 3) Reranker debugability and observability

- [x] Per-candidate score traces persisted (`reranker_score_trace_events`)
- [x] Debug endpoints available:
  - `/api/v1/matching/reranker/trace/{matchUuid}`
  - `/api/v1/matching/reranker/request/{requestId}`
- [x] Dashboard SQL covers concentration, conversion, TTFC, pre/post drift

### 4) Ops hardening

- [x] Collaborative trainer job supports multi-model/multi-segment and artifact retention
- [x] Compose smoke script validates local/proxy orchestrator modes
- [x] Degraded-mode runbook added

## Validation Commands

### TypeScript orchestrator

```bash
cd services/ml-orchestrator-ts
npm run typecheck
npm test
npm run build
```

### Python media adapters

```bash
python3 -m py_compile services/media-service/scripts/*.py
python3 -m pytest -q services/media-service/test_main.py
```

### Java backend

```bash
./mvnw -DskipTests package
```

## Rollout Notes

1. Keep all new behavior behind feature flags initially.
2. Start with proxy-only mode and enable local features by segment.
3. Monitor guardrails:
   - conversations/impression
   - report/block rates
   - concentration top-decile share
4. Use staged ramp with auto rollback (`manage_reranker_rollout.py`).
