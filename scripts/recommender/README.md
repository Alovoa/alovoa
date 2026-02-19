# Collaborative Prior Trainer (implicit / lightfm)

This folder contains optional offline tooling to compute collaborative priors for the reranker.

## Install

```bash
python3 -m pip install -r scripts/recommender/requirements.txt
```

## Run

```bash
python3 scripts/recommender/train_cf_priors.py \
  --db-url "mysql+pymysql://alovoa:alovoa@localhost:3306/alovoa" \
  --model implicit \
  --segment-key "*" \
  --output-sql /tmp/user_collaborative_prior.sql
```

Then apply generated SQL:

```bash
mysql -ualovoa -palovoa alovoa < /tmp/user_collaborative_prior.sql
```

Orchestrated multi-model + retention run via Java job runner:

```bash
cd /Users/tkhan/IdeaProjects/alovoa
CF_DB_URL="mysql+pymysql://alovoa:alovoa@localhost:3306/alovoa" \
CF_MODELS="implicit,lightfm" \
CF_SEGMENT_KEYS="*,segment:us_f_25_34_nyc" \
CF_RETENTION_DAYS=30 \
java -cp target/classes com.nonononoki.alovoa.jobs.ml.MlJobsRunner train-cf-priors
```

## Notes

- Safe by default: reranker ignores collaborative priors unless `enableCollaborativePrior=true` in `feature_flags.json_config` for `MATCH_RERANKER`.
- Table migration: `src/main/resources/db/migration/V18__collaborative_priors.sql`.

## SCUT score calibration

Build percentile calibration for attractiveness blending:

```bash
python3 scripts/recommender/build_scut_calibration.py \
  --repo-root third_party/repos/SCUT-FBP5500-Database-Release \
  --output-json configs/attractiveness/scut_score_calibration.json \
  --quantiles 201
```

Then enable in media-service:
- `ATTRACTIVENESS_CALIBRATION_ENABLE=true`
- `ATTRACTIVENESS_CALIBRATION_FILE=configs/attractiveness/scut_score_calibration.json`

## Reranker rollout guardrails

Evaluate and optionally apply staged rollout updates for `MATCH_RERANKER`:

```bash
python3 scripts/recommender/manage_reranker_rollout.py \
  --db-url "mysql+pymysql://alovoa:alovoa@localhost:3306/alovoa" \
  --segment-key "*" \
  --window-hours 24 \
  --rollout-stages "1,10,50,100"
```

Apply traffic changes (advance/rollback) when action is produced:

```bash
python3 scripts/recommender/manage_reranker_rollout.py \
  --db-url "mysql+pymysql://alovoa:alovoa@localhost:3306/alovoa" \
  --segment-key "*" \
  --apply
```

## NSFW calibration from labeled outputs

Create per-segment moderation thresholds:

```bash
python3 scripts/recommender/build_nsfw_calibration.py \
  --input-jsonl /path/to/moderation_labels.jsonl \
  --output-json configs/moderation/nsfw_thresholds.json \
  --default-threshold 0.60 \
  --min-samples-per-segment 30
```

## Deepfake calibration from labeled outputs

Create per-segment deepfake thresholds:

```bash
python3 scripts/recommender/build_deepfake_calibration.py \
  --input-jsonl /path/to/deepfake_labels.jsonl \
  --output-json configs/moderation/deepfake_thresholds.json \
  --target-precision 0.90 \
  --min-samples-per-segment 30
```

## Offline reranker evaluation

Evaluate control vs treatment ranking snapshots:

```bash
python3 scripts/recommender/evaluate_reranker_offline.py \
  --input-csv /path/to/reranker_eval_input.csv \
  --output-json logs/reranker-eval/latest_report.json \
  --k 20
```

Expected columns:
- `user_id`
- `candidate_id`
- `label`
- `score_control`
- `score_treatment`
- `segment_key` (optional)
