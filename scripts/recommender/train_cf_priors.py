#!/usr/bin/env python3
"""
Train collaborative priors from interaction logs and emit SQL upserts.

Supports:
- implicit (ALS)
- lightfm (WARP)

Example:
  python scripts/recommender/train_cf_priors.py \
    --db-url "mysql+pymysql://alovoa:alovoa@localhost:3306/alovoa" \
    --model implicit \
    --segment-key "*" \
    --output-sql /tmp/user_collaborative_prior.sql
"""

from __future__ import annotations

import argparse
import math
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Dict, Tuple, List

import numpy as np
import pandas as pd
from scipy.sparse import coo_matrix, csr_matrix
from sqlalchemy import create_engine, text


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


@dataclass
class ModelOutput:
    user_scores: np.ndarray
    model_name: str


def fetch_interactions(db_url: str, since_days: int) -> pd.DataFrame:
    engine = create_engine(db_url)
    since_ts = datetime.now(timezone.utc) - timedelta(days=since_days)

    sql = text(
        """
        SELECT viewer_id AS user_id, candidate_id AS item_id, 0.10 AS weight
        FROM impression_events
        WHERE ts >= :since_ts
        UNION ALL
        SELECT viewer_id AS user_id, candidate_id AS item_id, 1.00 AS weight
        FROM like_events
        WHERE ts >= :since_ts
        UNION ALL
        SELECT user_a AS user_id, user_b AS item_id, 2.00 AS weight
        FROM match_events
        WHERE ts >= :since_ts
        UNION ALL
        SELECT user_b AS user_id, user_a AS item_id, 2.00 AS weight
        FROM match_events
        WHERE ts >= :since_ts
        """
    )

    with engine.begin() as conn:
        df = pd.read_sql(sql, conn, params={"since_ts": since_ts})
    return df


def build_matrix(df: pd.DataFrame) -> Tuple[csr_matrix, np.ndarray, np.ndarray, np.ndarray]:
    if df.empty:
        return csr_matrix((0, 0)), np.array([]), np.array([]), np.array([])

    grouped = df.groupby(["user_id", "item_id"], as_index=False)["weight"].sum()
    user_ids = np.sort(grouped["user_id"].unique())
    item_ids = np.sort(grouped["item_id"].unique())

    user_to_idx = {u: i for i, u in enumerate(user_ids)}
    item_to_idx = {it: i for i, it in enumerate(item_ids)}

    rows = grouped["user_id"].map(user_to_idx).to_numpy(dtype=np.int64)
    cols = grouped["item_id"].map(item_to_idx).to_numpy(dtype=np.int64)
    data = grouped["weight"].to_numpy(dtype=np.float32)

    matrix = coo_matrix((data, (rows, cols)), shape=(len(user_ids), len(item_ids))).tocsr()

    user_interactions = np.asarray(matrix.sum(axis=1)).ravel()
    return matrix, user_ids, item_ids, user_interactions


def train_implicit(matrix: csr_matrix, factors: int, reg: float, iterations: int) -> ModelOutput:
    import implicit  # type: ignore

    model = implicit.als.AlternatingLeastSquares(
        factors=factors,
        regularization=reg,
        iterations=iterations,
        use_gpu=False,
    )
    model.fit(matrix)

    norms = np.linalg.norm(model.user_factors, axis=1)
    if float(np.max(norms)) > 0.0:
        scores = norms / float(np.max(norms))
    else:
        scores = np.full_like(norms, 0.5, dtype=np.float32)
    return ModelOutput(user_scores=scores.astype(np.float32), model_name="implicit_als")


def train_lightfm(matrix: csr_matrix, factors: int, epochs: int) -> ModelOutput:
    from lightfm import LightFM  # type: ignore

    model = LightFM(no_components=factors, loss="warp")
    model.fit(matrix, epochs=epochs, num_threads=4)

    user_emb = model.user_embeddings
    norms = np.linalg.norm(user_emb, axis=1)
    if float(np.max(norms)) > 0.0:
        scores = norms / float(np.max(norms))
    else:
        scores = np.full_like(norms, 0.5, dtype=np.float32)
    return ModelOutput(user_scores=scores.astype(np.float32), model_name="lightfm_warp")


def confidence_from_interactions(interactions: np.ndarray, n0: float) -> np.ndarray:
    interactions = np.maximum(interactions, 0.0)
    return interactions / (interactions + max(1e-9, n0))


def build_upserts(
    user_ids: np.ndarray,
    prior_scores: np.ndarray,
    confidences: np.ndarray,
    segment_key: str,
    model_name: str,
) -> List[str]:
    statements: List[str] = []
    for user_id, prior, conf in zip(user_ids, prior_scores, confidences):
        prior_v = clamp01(float(prior))
        conf_v = clamp01(float(conf))
        statements.append(
            "INSERT INTO user_collaborative_prior "
            "(user_id, segment_key, prior_score, confidence, source_model) "
            f"VALUES ({int(user_id)}, '{segment_key}', {prior_v:.6f}, {conf_v:.6f}, '{model_name}') "
            "ON DUPLICATE KEY UPDATE "
            "prior_score = VALUES(prior_score), "
            "confidence = VALUES(confidence), "
            "source_model = VALUES(source_model), "
            "updated_at = CURRENT_TIMESTAMP;"
        )
    return statements


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--db-url", required=True, help="SQLAlchemy DB URL")
    parser.add_argument("--model", choices=["implicit", "lightfm"], default="implicit")
    parser.add_argument("--segment-key", default="*")
    parser.add_argument("--since-days", type=int, default=90)
    parser.add_argument("--factors", type=int, default=32)
    parser.add_argument("--regularization", type=float, default=0.01)
    parser.add_argument("--iterations", type=int, default=20)
    parser.add_argument("--epochs", type=int, default=20)
    parser.add_argument("--n0", type=float, default=200.0)
    parser.add_argument("--output-sql", required=True)
    args = parser.parse_args()

    df = fetch_interactions(args.db_url, args.since_days)
    matrix, user_ids, _, user_interactions = build_matrix(df)
    if matrix.shape[0] == 0:
        Path(args.output_sql).write_text("-- No interactions found; nothing to upsert.\n", encoding="utf-8")
        return 0

    if args.model == "implicit":
        out = train_implicit(matrix, args.factors, args.regularization, args.iterations)
    else:
        out = train_lightfm(matrix, args.factors, args.epochs)

    conf = confidence_from_interactions(user_interactions, args.n0)
    upserts = build_upserts(user_ids, out.user_scores, conf, args.segment_key, out.model_name)

    output_path = Path(args.output_sql)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text("\n".join(upserts) + "\n", encoding="utf-8")
    print(f"Wrote {len(upserts)} upserts to {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

