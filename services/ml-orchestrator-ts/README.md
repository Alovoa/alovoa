# AURA ML Orchestrator (TypeScript)

TypeScript control plane for media/ML features. It keeps heavyweight model inference in Python workers while moving routing, policy, rollout, and integration logic into TS.

## Why this exists

- Keep runtime flexibility with swappable providers.
- Preserve existing Java backend contracts.
- Add feature controls for OSS model stacks without hard-coding one architecture.

## Endpoints

Compatible media endpoints:

- `GET /health`
- `POST /verify/liveness/challenges`
- `POST /verify/face`
- `POST /video/analyze`
- `POST /attractiveness/score`
- `POST /moderation/image`
- `POST /moderation/text`
- `POST /quality/face`
- `POST /video/transcribe` (proxy)
- `POST /video/analyze-transcript` (proxy)
- `GET /integrations/status`
- `GET /integrations/qdrant/candidate-enrichment?user_id=<id>&segment_key=<key>&limit=<n>`

## Provider model

Default mode is `proxy` to Python media-service (`PYTHON_MEDIA_SERVICE_URL`).

Optional local TS policy modes:

- `ENABLE_LOCAL_MODERATION=true`
- `ENABLE_LOCAL_TEXT_MODERATION=true`
- `ENABLE_LOCAL_FACE_QUALITY=true`
- `ENABLE_LOCAL_ATTRACTIVENESS=true`

These use script adapters wired to OSS repos and fallback to Python proxy on failure.

## OSS repo feature mapping

Implemented now:

- `deepface` / `insightface` / `mediapipe` / `face-alignment` / `dlib`
  - Active in Python media-service pipeline, orchestrated by TS.
- `Silent-Face-Anti-Spoofing`
  - Script adapter path for anti-spoof (`services/media-service/scripts/silent_face_antispoof_adapter.py`).
- `opennsfw2` / `NudeNet`
  - Script adapter path for moderation (`services/media-service/scripts/nsfw_*.py`).
- `unitaryai/detoxify`
  - Text toxicity adapter path (`services/media-service/scripts/text_detoxify_adapter.py`).
- `LSIbabnikz/FaceQAN`
  - Face-quality adapter path (`services/media-service/scripts/faceqan_adapter.py`).
- `LAION-AI/CLIP-based-NSFW-Detector`
  - CLIP NSFW adapter path (`services/media-service/scripts/nsfw_clip_adapter.py`).
- `SCLBD/DeepfakeBench`
  - Offline deepfake-threshold calibration job wrapper (`src/jobs/buildDeepfakeCalibration.ts`).
- `recommenders-team/recommenders`
  - Offline reranker evaluation job wrapper (`src/jobs/evaluateRerankerOffline.ts`).
- `implicit` / `lightfm`
  - Offline collaborative prior training job wrapper in TS (`src/jobs/trainCollaborativePriors.ts`).
- `openfga` / `unleash` / `qdrant`
  - Integrated health/status clients in TS (`/integrations/status`), ready for policy expansion.

Research/offline references retained:

- `BeautyPredict`, `ComboLoss`, `FaceAttract`, `MetaFBP`, `SCUT-FBP5500`
- `DECA`, `Deep3DFaceRecon_pytorch`

## Environment variables

Core:

- `PORT` (default `8081`)
- `PYTHON_MEDIA_SERVICE_URL` (default `http://localhost:8001`)
- `REQUEST_TIMEOUT_MS` (default `15000`)

Moderation local mode:

- `ENABLE_LOCAL_MODERATION` (`false` default)
- `NSFW_USE_OPENNSFW2`, `NSFW_USE_NUDENET`, `NSFW_USE_CLIP_NSFW`
- `NSFW_OPENNSFW2_CMD`, `NSFW_NUDENET_CMD`, `NSFW_CLIP_CMD`
- `NSFW_THRESHOLD`, `NSFW_BASELINE`
- per-segment policy overrides:
  - `NSFW_SEGMENT_POLICIES_JSON`
  - example:
    - `{"us_f_25_34_nyc":{"threshold":0.52,"baseline":0.03,"openNsfw2Weight":0.3,"nudeNetWeight":0.5,"clipNsfwWeight":0.2}}`
- calibration file:
  - `NSFW_CALIBRATION_ENABLE=true`
  - `NSFW_CALIBRATION_FILE=/absolute/path/to/nsfw_thresholds.json`
- moderation policy gating (optional):
  - `MODERATION_USE_UNLEASH_FLAG=true`
  - `MODERATION_UNLEASH_FLAG_NAME=moderation_local_enabled`
  - `MODERATION_OPENFGA_ENFORCE=true`
  - `MODERATION_OPENFGA_OBJECT=feature:moderation_local`
  - `MODERATION_OPENFGA_RELATION=can_use`
- safety signal traces (optional JSONL logging):
  - `SAFETY_SIGNAL_LOG_ENABLE=true`
  - `SAFETY_SIGNAL_LOG_FILE=/absolute/path/to/safety_signal_events.jsonl`

Text moderation local mode:

- `ENABLE_LOCAL_TEXT_MODERATION` (`false` default)
- `TEXT_MODERATION_CMD`
  - default: `python3 services/media-service/scripts/text_detoxify_adapter.py --text {text}`
- `TEXT_MODERATION_MODEL` (default `multilingual`)
- thresholds:
  - `TEXT_MODERATION_BLOCK_THRESHOLD` (default `0.72`)
  - `TEXT_MODERATION_WARN_THRESHOLD` (default `0.52`)
- per-segment policy overrides:
  - `TEXT_MODERATION_SEGMENT_POLICIES_JSON`
  - example:
    - `{"us_f_25_34_nyc":{"blockThreshold":0.68,"warnThreshold":0.50}}`
- rollout/entitlements (optional):
  - `TEXT_MODERATION_USE_UNLEASH_FLAG=true`
  - `TEXT_MODERATION_UNLEASH_FLAG_NAME=moderation_text_local_enabled`
  - `TEXT_MODERATION_OPENFGA_ENFORCE=true`
  - `TEXT_MODERATION_OPENFGA_OBJECT=feature:moderation_text_local`
  - `TEXT_MODERATION_OPENFGA_RELATION=can_use`

Face quality local mode:

- `ENABLE_LOCAL_FACE_QUALITY` (`false` default)
- `FACE_QUALITY_CMD`
  - default: `python3 services/media-service/scripts/faceqan_adapter.py --image {image_path}`
- `FACE_QUALITY_PROVIDER` (default `faceqan`)
- `FACE_QUALITY_MODEL_VERSION` (default `faceqan_v1`)
- rollout/entitlements (optional):
  - `FACE_QUALITY_USE_UNLEASH_FLAG=true`
  - `FACE_QUALITY_UNLEASH_FLAG_NAME=face_quality_gate_enabled`
  - `FACE_QUALITY_OPENFGA_ENFORCE=true`
  - `FACE_QUALITY_OPENFGA_OBJECT=feature:face_quality_gate`
  - `FACE_QUALITY_OPENFGA_RELATION=can_use`

Attractiveness local mode:

- `ENABLE_LOCAL_ATTRACTIVENESS` (`false` default)
- `ATTRACTIVENESS_CMD`
  - default: `python3 services/media-service/scripts/attractiveness_oss_adapter.py --image {image_path} --view {view}`
  - adapter uses `insightface` (if installed), optional `mediapipe`, and CV fallbacks
- `ATTRACTIVENESS_FRONT_WEIGHT`, `ATTRACTIVENESS_SIDE_WEIGHT`
- `ATTRACTIVENESS_BASELINE`, `ATTRACTIVENESS_PROVIDER`, `ATTRACTIVENESS_MODEL_VERSION`
- calibration:
  - `ATTRACTIVENESS_CALIBRATION_ENABLE=true`
  - `ATTRACTIVENESS_CALIBRATION_FILE=/absolute/path/to/scut_score_calibration.json`
  - default bundled calibration file:
    - `configs/attractiveness/scut_score_calibration.json`
- rollout/policy gates (optional):
  - `ATTRACTIVENESS_USE_UNLEASH_FLAG=true`
  - `ATTRACTIVENESS_UNLEASH_FLAG_NAME=attractiveness_local_enabled`
  - `ATTRACTIVENESS_OPENFGA_ENFORCE=true`
  - `ATTRACTIVENESS_OPENFGA_OBJECT=feature:attractiveness_local`
  - `ATTRACTIVENESS_OPENFGA_RELATION=can_use`
- Qdrant hinting (optional):
  - `QDRANT_ATTRACTIVENESS_HINT_ENABLE=true`
  - `QDRANT_ATTRACTIVENESS_COLLECTION=attractiveness_hints`
  - `QDRANT_ATTRACTIVENESS_HINT_MAX_DELTA=0.08`
- Qdrant candidate enrichment (optional):
  - `QDRANT_CANDIDATE_ENRICHMENT_ENABLE=true`
  - `QDRANT_CANDIDATE_COLLECTION=candidate_enrichment`
  - `QDRANT_CANDIDATE_ENRICHMENT_LIMIT=20`
- `ATTRACTIVENESS_PROVIDER_CMDS` (JSON array; optional multi-provider ensemble)
  - example:
    - `[{"name":"comboloss","cmd":"python3 .../attractiveness_comboloss_adapter.py --image {image_path} --model /models/combo.pth","weight":0.35,"model_version":"combo_custom_v2"}]`
- `ATTRACTIVENESS_PROVIDER_AB_RATES` (JSON object for deterministic per-provider rollout on contributions)
  - example:
    - `{"comboloss":0.5,"deca_geometry":0.2,"face_alignment_geometry":0.1}`
  - rollout is deterministic by `user_id` (or `segment_key` fallback)
- Auto-wiring shortcuts:
  - `COMBOLOSS_MODEL_PATH` (enables default ComboLoss adapter command)
  - `FACIAL_BEAUTY_MODEL_PATH` (enables default facial-beauty-prediction adapter command)
  - `BEAUTYPREDICT_MODEL_PATH` (enables default BeautyPredict adapter command)
  - `FACEATTRACT_MODEL_PATH` (enables default FaceAttract adapter command)
  - `METAFBP_MODEL_PATH` (enables default MetaFBP adapter command)
  - `DECA_ENABLE=true` (enables default DECA geometry adapter command)
  - or `DECA_CMD` to provide a custom DECA command
  - `DEEP3D_ENABLE=true` (enables default Deep3D geometry adapter command)
  - or `DEEP3D_CMD` to provide a custom Deep3D command
  - `ATTRACTIVENESS_INSIGHTFACE_ENABLE=true` (enables dedicated InsightFace geometry adapter)
  - or `ATTRACTIVENESS_INSIGHTFACE_CMD` to provide a custom InsightFace command
  - `ATTRACTIVENESS_MEDIAPIPE_ENABLE=true` (enables dedicated MediaPipe geometry adapter)
  - or `ATTRACTIVENESS_MEDIAPIPE_CMD` to provide a custom MediaPipe command
  - `ATTRACTIVENESS_DEEPFACE_ENABLE=true` (enables dedicated DeepFace quality adapter)
  - or `ATTRACTIVENESS_DEEPFACE_CMD` to provide a custom DeepFace command
  - `FACE_ALIGNMENT_ENABLE=true` (enables default face-alignment geometry adapter command)
  - or `FACE_ALIGNMENT_CMD` to provide a custom face-alignment command
  - `DLIB_ENABLE=true` (enables default dlib geometry adapter command)
  - or `DLIB_CMD` to provide a custom dlib command
  - optional weights:
    - `COMBOLOSS_WEIGHT`
    - `FACIAL_BEAUTY_WEIGHT`
    - `BEAUTYPREDICT_WEIGHT`
    - `FACEATTRACT_WEIGHT`
    - `METAFBP_WEIGHT`
    - `DECA_WEIGHT`
    - `DEEP3D_WEIGHT`
    - `ATTRACTIVENESS_INSIGHTFACE_WEIGHT`
    - `ATTRACTIVENESS_MEDIAPIPE_WEIGHT`
    - `ATTRACTIVENESS_DEEPFACE_WEIGHT`
    - `FACE_ALIGNMENT_WEIGHT`
    - `DLIB_WEIGHT`
  - optional model versions:
    - `COMBOLOSS_MODEL_VERSION`
    - `FACIAL_BEAUTY_MODEL_VERSION`
    - `BEAUTYPREDICT_MODEL_VERSION`
    - `FACEATTRACT_MODEL_VERSION`
    - `METAFBP_MODEL_VERSION`
    - `DECA_MODEL_VERSION`
    - `DEEP3D_MODEL_VERSION`
    - `ATTRACTIVENESS_INSIGHTFACE_MODEL_VERSION`
    - `ATTRACTIVENESS_MEDIAPIPE_MODEL_VERSION`
    - `ATTRACTIVENESS_DEEPFACE_MODEL_VERSION`
    - `FACE_ALIGNMENT_MODEL_VERSION`
    - `DLIB_MODEL_VERSION`

Attractiveness responses include:
- `provider_versions` (map of provider contribution names to resolved model versions)

Integrations (optional):

- `ENABLE_QDRANT`, `QDRANT_URL`, `QDRANT_API_KEY`
- `ENABLE_UNLEASH`, `UNLEASH_URL`, `UNLEASH_API_TOKEN`, `UNLEASH_APP_NAME`
- `ENABLE_OPENFGA`, `OPENFGA_URL`, `OPENFGA_STORE_ID`, `OPENFGA_AUTHZ_MODEL_ID`, `OPENFGA_API_TOKEN`

When enabled, these integrations are used directly in attractiveness flow:
- Unleash: local-scoring rollout gate before running local providers.
- OpenFGA: entitlement check for local-scoring access.
- Qdrant: optional user-level score hint delta (bounded by max delta).

## Local run

```bash
cd services/ml-orchestrator-ts
npm install
npm run dev
```

## Build

```bash
cd services/ml-orchestrator-ts
npm run build
npm run start
```

## Collaborative prior job

```bash
cd services/ml-orchestrator-ts
CF_DB_URL="mysql+pymysql://alovoa:alovoa@localhost:3306/alovoa" \
npm run job:train-cf-priors
```

This invokes `scripts/recommender/train_cf_priors.py` and emits SQL upserts.

Multi-run and retention controls:
- `CF_MODELS=implicit,lightfm` (or `CF_MODEL=both`)
- `CF_SEGMENT_KEYS=*,segment:us_f_25_34_nyc`
- `CF_ARTIFACT_DIR=/absolute/path/to/logs/cf-priors`
- `CF_RETENTION_DAYS=30`
- `CF_KEEP_MIN_FILES=20`

## SCUT calibration job

```bash
cd services/ml-orchestrator-ts
npm run job:build-scut-calibration
```

This invokes `scripts/recommender/build_scut_calibration.py` and writes:
- `configs/attractiveness/scut_score_calibration.json` (default)

## Reranker rollout guardrail job

```bash
cd services/ml-orchestrator-ts
ROLLOUT_DB_URL="mysql+pymysql://alovoa:alovoa@localhost:3306/alovoa" \
npm run job:manage-reranker-rollout
```

Optional apply mode:

```bash
cd services/ml-orchestrator-ts
ROLLOUT_DB_URL="mysql+pymysql://alovoa:alovoa@localhost:3306/alovoa" \
ROLLOUT_APPLY=true \
npm run job:manage-reranker-rollout
```

## NSFW calibration job

```bash
cd services/ml-orchestrator-ts
NSFW_CALIBRATION_INPUT_JSONL=/absolute/path/to/moderation_labels.jsonl \
npm run job:build-nsfw-calibration
```

Writes default output:
- `configs/moderation/nsfw_thresholds.json`

## Deepfake calibration job

```bash
cd services/ml-orchestrator-ts
DEEPFAKE_CALIBRATION_INPUT_JSONL=/absolute/path/to/deepfake_labels.jsonl \
npm run job:build-deepfake-calibration
```

Writes default output:
- `configs/moderation/deepfake_thresholds.json`

## Offline reranker evaluation job

```bash
cd services/ml-orchestrator-ts
RERANKER_EVAL_INPUT_CSV=/absolute/path/to/reranker_eval_input.csv \
npm run job:evaluate-reranker-offline
```

Writes default output:
- `logs/reranker-eval/latest_report.json`

## Compose smoke checks

Run orchestrator smoke checks in both proxy and local modes:

```bash
./scripts/smoke/smoke_orchestrator_modes.sh all
```

Runbook:
- `docs/runbooks/ml-orchestrator-degraded-mode.md`
