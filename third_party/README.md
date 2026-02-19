# Third-Party Repos (Research + Porting)

These repositories were cloned for reference and selective porting into AURA.

Location:
- `third_party/repos`

Use policy:
- Keep upstream code isolated in `third_party/repos`.
- Port only required modules into first-party code under explicit adapters.
- Respect each upstream repository's license and model usage terms before production use.

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

## Ported Into Backend

1. Hidden visual-attractiveness scoring endpoint in `services/media-service/main.py`:
   - OSS providers: `deepface` + optional `insightface`
   - optional landmark symmetry via `mediapipe` if installed
   - optional external scorer command hook to run repo-specific models (ComboLoss / BeautyPredict / FaceAttract / MetaFBP / DECA / Deep3DFaceRecon)
   - example adapter: `services/media-service/scripts/external_scorer_example.py`
   - output is backend-only and persisted as a ranking prior
2. Backend sync + blend:
   - `user_visual_attractiveness` table
   - scheduled scorer + blend into `user_rolling_stats.backend_attractiveness_score`

## Next Porting Targets

1. Two-tower front/side regression model training pipeline (SCUT-FBP/ComboLoss-style experiments)
2. Optional advanced geometry priors (DECA / Deep3DFaceRecon) in offline evaluation only
