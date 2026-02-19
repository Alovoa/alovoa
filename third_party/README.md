# Third-Party Repos (Research + Porting)

These repositories were cloned for reference and selective porting into AURA.

Location:
- `third_party/repos`

Use policy:
- Keep upstream code isolated in `third_party/repos`.
- Port only required modules into first-party code under explicit adapters.
- Respect each upstream repository's license and model usage terms before production use.

Progress tracking:
- Master tracked plan and completion status lives in:
  - `/Users/tkhan/IdeaProjects/alovoa/docs/oss-ts-implementation-plan.md`

## Cloned Repositories

| Repo | URL | Primary Purpose |
|---|---|---|
| SCUT-FBP5500-Database-Release | https://github.com/HCIILAB/SCUT-FBP5500-Database-Release | Benchmark/dataset reference for beauty prediction |
| ComboLoss | https://github.com/lucasxlu/ComboLoss | Facial attractiveness model baseline |
| BeautyPredict | https://github.com/ustcqidi/BeautyPredict | Attractiveness prediction baseline |
| FaceAttract | https://github.com/fei-aiart/FaceAttract | Attractiveness model/pipeline reference |
| MetaFBP | https://github.com/MetaVisionLab/MetaFBP | Personalized facial beauty prediction reference |
| facial-beauty-prediction | https://github.com/etrain-xyz/facial-beauty-prediction | End-to-end benchmark implementation |
| mediapipe | https://github.com/google-ai-edge/mediapipe | Landmarking and face mesh pipeline |
| dlib | https://github.com/davisking/dlib | Classical landmarking baseline |
| face-alignment | https://github.com/1adrianb/face-alignment | 2D/3D face alignment |
| insightface | https://github.com/deepinsight/insightface | Face analysis toolbox |
| deepface | https://github.com/serengil/deepface | Face analysis wrapper/pipeline shortcuts |
| DECA | https://github.com/yfeng95/DECA | 3D face reconstruction |
| Deep3DFaceRecon_pytorch | https://github.com/sicxu/Deep3DFaceRecon_pytorch | 3D face reconstruction baseline |
| Silent-Face-Anti-Spoofing | https://github.com/minivision-ai/Silent-Face-Anti-Spoofing | Silent liveness / anti-spoof detection baseline |
| opennsfw2 | https://github.com/bhky/opennsfw2 | NSFW probability scoring baseline |
| NudeNet | https://github.com/notAI-tech/NudeNet | Nudity/object-level moderation baseline |
| implicit | https://github.com/benfred/implicit | Implicit-feedback collaborative filtering (ALS/BPR) |
| lightfm | https://github.com/lyst/lightfm | Hybrid recommender training baseline |
| openfga | https://github.com/openfga/openfga | Fine-grained authorization / entitlements reference |
| unleash | https://github.com/Unleash/unleash | Feature flag platform reference |
| qdrant | https://github.com/qdrant/qdrant | Vector retrieval backend reference |
| detoxify | https://github.com/unitaryai/detoxify | Text toxicity scoring models for moderation |
| FaceQAN | https://github.com/LSIbabnikz/FaceQAN | Face image quality assessment (FIQA) signal |
| CLIP-based-NSFW-Detector | https://github.com/LAION-AI/CLIP-based-NSFW-Detector | CLIP-embedding NSFW probability model |
| DeepfakeBench | https://github.com/SCLBD/DeepfakeBench | Deepfake detection benchmark/training framework |
| recommenders | https://github.com/recommenders-team/recommenders | Recommender evaluation baselines and metrics toolkit |

## Ported Into Backend

1. Hidden visual-attractiveness scoring endpoint in `services/media-service/main.py`:
   - OSS providers: `deepface` + optional `insightface`
   - optional landmark symmetry via `mediapipe` if installed
   - optional external scorer command hook to run repo-specific models (ComboLoss / BeautyPredict / FaceAttract / MetaFBP / DECA / Deep3DFaceRecon)
   - default OSS adapter for local orchestration: `services/media-service/scripts/attractiveness_oss_adapter.py`
   - reference adapter: `services/media-service/scripts/external_scorer_example.py`
   - repo-specific adapters for direct plug-in:
     - `services/media-service/scripts/attractiveness_comboloss_adapter.py`
     - `services/media-service/scripts/attractiveness_facial_beauty_adapter.py`
     - `services/media-service/scripts/attractiveness_beautypredict_adapter.py`
     - `services/media-service/scripts/attractiveness_faceattract_adapter.py`
     - `services/media-service/scripts/attractiveness_metafbp_adapter.py`
     - `services/media-service/scripts/attractiveness_deca_geometry_adapter.py`
     - `services/media-service/scripts/attractiveness_deep3d_geometry_adapter.py`
     - `services/media-service/scripts/attractiveness_insightface_adapter.py`
     - `services/media-service/scripts/attractiveness_mediapipe_geometry_adapter.py`
     - `services/media-service/scripts/attractiveness_deepface_adapter.py`
     - `services/media-service/scripts/attractiveness_face_alignment_adapter.py`
     - `services/media-service/scripts/attractiveness_dlib_geometry_adapter.py`
   - output is backend-only and persisted as a ranking prior
2. Anti-spoof + deepfake hardening hooks in `services/media-service/main.py`:
   - optional external anti-spoof command (`ANTISPOOF_EXTERNAL_CMD`) for Silent-Face integration
   - adapter script: `services/media-service/scripts/silent_face_antispoof_adapter.py`
3. Image moderation endpoint in `services/media-service/main.py`:
   - endpoint: `POST /moderation/image`
   - optional OpenNSFW2/NudeNet integration and external scorer command
   - adapter scripts:
     - `services/media-service/scripts/nsfw_opennsfw2_adapter.py`
     - `services/media-service/scripts/nsfw_nudenet_adapter.py`
   - per-segment moderation policy overrides + calibration:
     - `NSFW_SEGMENT_POLICIES_JSON`
     - `NSFW_CALIBRATION_ENABLE=true`
     - calibration builder: `scripts/recommender/build_nsfw_calibration.py`
4. Backend sync + blend:
   - `user_visual_attractiveness` table
   - scheduled scorer + blend into `user_rolling_stats.backend_attractiveness_score`
5. Optional collaborative prior support for reranker:
   - DB: `user_collaborative_prior` (migration `V18__collaborative_priors.sql`)
   - read path: `RollingStatsReadService`
   - policy: `CollaborativePriorPolicy`
   - offline trainer script:
     - `scripts/recommender/train_cf_priors.py`
   - sources: `implicit` / `lightfm` repos (opt-in)
6. Java-first ML job orchestration:
   - Java runner: `src/main/java/com/nonononoki/alovoa/jobs/ml/MlJobsRunner.java`
   - wraps offline scripts:
     - `scripts/recommender/build_scut_calibration.py`
     - `scripts/recommender/build_nsfw_calibration.py`
     - `scripts/recommender/build_deepfake_calibration.py`
     - `scripts/recommender/evaluate_reranker_offline.py`
     - `scripts/recommender/manage_reranker_rollout.py`
     - `scripts/recommender/train_cf_priors.py`
7. Infra integrations wired into Java runtime behavior:
   - Qdrant hinting + candidate enrichment clients:
     - `src/main/java/com/nonononoki/alovoa/service/ml/QdrantIntegrationService.java`
   - Unleash flag client:
     - `src/main/java/com/nonononoki/alovoa/service/ml/UnleashIntegrationService.java`
   - OpenFGA entitlement client:
     - `src/main/java/com/nonononoki/alovoa/service/ml/OpenFgaIntegrationService.java`
   - applied in attractiveness sync for rollout gating + bounded score hinting:
     - `src/main/java/com/nonononoki/alovoa/matching/rerank/service/VisualAttractivenessSyncService.java`
8. Text moderation stack (Detoxify):
   - media-service endpoint: `POST /moderation/text`
   - script adapter: `services/media-service/scripts/text_detoxify_adapter.py`
   - Java moderation chain wired in:
     - `src/main/java/com/nonononoki/alovoa/service/ContentModerationService.java`
9. Face quality stack (FaceQAN-inspired):
   - media-service endpoint: `POST /quality/face`
   - script adapter: `services/media-service/scripts/faceqan_adapter.py`
   - adapter-blended quality signal in media-service scoring:
     - `services/media-service/main.py`
   - persisted quality events + quality-gated attractiveness sync:
     - `src/main/resources/db/migration/V21__face_quality_events.sql`
     - `src/main/java/com/nonononoki/alovoa/matching/rerank/service/FaceQualityScoringService.java`
     - `src/main/java/com/nonononoki/alovoa/matching/rerank/service/VisualAttractivenessSyncService.java`
10. CLIP NSFW + offline calibration/eval:
   - CLIP NSFW adapter:
     - `services/media-service/scripts/nsfw_clip_adapter.py`
   - moderation tri-ensemble now supports:
     - OpenNSFW2 + NudeNet + CLIP NSFW (`services/media-service/main.py`)
   - offline deepfake calibration:
     - `scripts/recommender/build_deepfake_calibration.py`
     - Java runner job: `build-deepfake-calibration`
   - offline reranker evaluation:
     - `scripts/recommender/evaluate_reranker_offline.py`
     - Java runner job: `evaluate-reranker-offline`

## Remaining Optional Targets (Post-V1)

1. Two-tower front/side regression model training experiments for improved calibration (offline R&D).
2. Additional DECA/Deep3D geometry feature engineering for offline evaluation sets.
3. Expansion of OpenFGA/Unleash/Qdrant policies beyond current attractiveness/matching hooks.

## Fit Review

Detailed fit-together assessment for newly cloned moderation/quality/recommender repos:
- `/Users/tkhan/IdeaProjects/alovoa/docs/oss-fit-together-review.md`
