#!/usr/bin/env python3
"""
Offline reranker evaluator.

Expected CSV columns:
- user_id
- candidate_id
- label (0/1)
- score_control
- score_treatment
- segment_key (optional)

Outputs control/treatment metrics and deltas in JSON.
"""

from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Tuple

import numpy as np
import pandas as pd


@dataclass
class MetricPack:
    precision_at_k: float
    recall_at_k: float
    ndcg_at_k: float
    map_at_k: float
    coverage_at_k: float
    diversity_entropy_at_k: float


def _dcg(rels: List[int]) -> float:
    out = 0.0
    for i, rel in enumerate(rels, start=1):
        if rel <= 0:
            continue
        out += float(rel) / float(np.log2(i + 1))
    return out


def _ndcg_at_k(rels: List[int], k: int) -> float:
    rels = rels[:k]
    if not rels:
        return 0.0
    dcg = _dcg(rels)
    ideal = sorted(rels, reverse=True)
    idcg = _dcg(ideal)
    if idcg <= 0:
        return 0.0
    return dcg / idcg


def _ap_at_k(rels: List[int], k: int) -> float:
    rels = rels[:k]
    hits = 0
    precision_sum = 0.0
    for i, rel in enumerate(rels, start=1):
        if rel > 0:
            hits += 1
            precision_sum += hits / i
    if hits == 0:
        return 0.0
    return precision_sum / hits


def evaluate(df: pd.DataFrame, score_col: str, label_col: str, user_col: str, item_col: str, k: int) -> MetricPack:
    by_user = []
    exposure: Dict[str, int] = {}
    all_items = set(df[item_col].astype(str).tolist())

    for _, group in df.groupby(user_col):
        ordered = group.sort_values(score_col, ascending=False)
        topk = ordered.head(k)
        rels = topk[label_col].fillna(0).astype(int).tolist()
        total_relevant = int(group[label_col].fillna(0).astype(int).sum())

        hits = int(sum(rels))
        precision = hits / k if k > 0 else 0.0
        recall = hits / total_relevant if total_relevant > 0 else 0.0
        ndcg = _ndcg_at_k(rels, k)
        ap = _ap_at_k(rels, k)

        by_user.append((precision, recall, ndcg, ap))

        for item in topk[item_col].astype(str).tolist():
            exposure[item] = exposure.get(item, 0) + 1

    if not by_user:
        return MetricPack(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

    precision_at_k = sum(x[0] for x in by_user) / len(by_user)
    recall_at_k = sum(x[1] for x in by_user) / len(by_user)
    ndcg_at_k = sum(x[2] for x in by_user) / len(by_user)
    map_at_k = sum(x[3] for x in by_user) / len(by_user)

    covered = len(exposure)
    coverage = covered / max(1, len(all_items))

    total_exposure = sum(exposure.values())
    entropy = 0.0
    if total_exposure > 0:
        for count in exposure.values():
            p = count / total_exposure
            if p > 0:
                entropy -= p * float(np.log2(p))

    max_entropy = float(np.log2(max(2, covered)))
    diversity = (entropy / max_entropy) if max_entropy > 0 else 0.0

    return MetricPack(
        precision_at_k=float(precision_at_k),
        recall_at_k=float(recall_at_k),
        ndcg_at_k=float(ndcg_at_k),
        map_at_k=float(map_at_k),
        coverage_at_k=float(coverage),
        diversity_entropy_at_k=float(diversity),
    )


def pack(m: MetricPack) -> Dict[str, float]:
    return {
        "precision_at_k": round(m.precision_at_k, 6),
        "recall_at_k": round(m.recall_at_k, 6),
        "ndcg_at_k": round(m.ndcg_at_k, 6),
        "map_at_k": round(m.map_at_k, 6),
        "coverage_at_k": round(m.coverage_at_k, 6),
        "diversity_entropy_at_k": round(m.diversity_entropy_at_k, 6),
    }


def delta(control: MetricPack, treatment: MetricPack) -> Dict[str, float]:
    return {
        "precision_at_k": round(treatment.precision_at_k - control.precision_at_k, 6),
        "recall_at_k": round(treatment.recall_at_k - control.recall_at_k, 6),
        "ndcg_at_k": round(treatment.ndcg_at_k - control.ndcg_at_k, 6),
        "map_at_k": round(treatment.map_at_k - control.map_at_k, 6),
        "coverage_at_k": round(treatment.coverage_at_k - control.coverage_at_k, 6),
        "diversity_entropy_at_k": round(treatment.diversity_entropy_at_k - control.diversity_entropy_at_k, 6),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-csv", required=True)
    parser.add_argument("--output-json", required=True)
    parser.add_argument("--k", type=int, default=20)
    parser.add_argument("--control-col", default="score_control")
    parser.add_argument("--treatment-col", default="score_treatment")
    parser.add_argument("--label-col", default="label")
    parser.add_argument("--user-col", default="user_id")
    parser.add_argument("--item-col", default="candidate_id")
    parser.add_argument("--group-col", default="segment_key")
    args = parser.parse_args()

    input_path = Path(args.input_csv).resolve()
    output_path = Path(args.output_json).resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)

    df = pd.read_csv(input_path)

    required = [
        args.user_col,
        args.item_col,
        args.label_col,
        args.control_col,
        args.treatment_col,
    ]
    missing = [c for c in required if c not in df.columns]
    if missing:
        raise SystemExit(f"Missing required columns: {missing}")

    control_metrics = evaluate(df, args.control_col, args.label_col, args.user_col, args.item_col, args.k)
    treatment_metrics = evaluate(df, args.treatment_col, args.label_col, args.user_col, args.item_col, args.k)

    by_segment: Dict[str, Dict[str, Dict[str, float]]] = {}
    if args.group_col in df.columns:
        for segment, seg_df in df.groupby(args.group_col):
            control_seg = evaluate(seg_df, args.control_col, args.label_col, args.user_col, args.item_col, args.k)
            treatment_seg = evaluate(seg_df, args.treatment_col, args.label_col, args.user_col, args.item_col, args.k)
            by_segment[str(segment)] = {
                "control": pack(control_seg),
                "treatment": pack(treatment_seg),
                "delta": delta(control_seg, treatment_seg),
                "rows": {"count": int(len(seg_df))},
            }

    payload = {
        "type": "reranker_offline_eval_v1",
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "source": str(input_path),
        "k": int(args.k),
        "metrics": {
            "control": pack(control_metrics),
            "treatment": pack(treatment_metrics),
            "delta": delta(control_metrics, treatment_metrics),
        },
        "segments": by_segment,
        "rows": {
            "count": int(len(df)),
            "users": int(df[args.user_col].nunique()),
            "items": int(df[args.item_col].nunique()),
        },
    }

    output_path.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    print(json.dumps({"output_json": str(output_path), "rows": len(df)}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
