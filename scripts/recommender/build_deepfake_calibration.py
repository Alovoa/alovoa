#!/usr/bin/env python3
"""
Build per-segment deepfake score thresholds from labeled events.

Input JSONL records may include:
- segment_key
- is_deepfake (0/1)
- deepfake_prob (0..1)

Fallback score fields:
- spoof_prob
- deepfake_score (interpreted as authenticity => prob = 1 - deepfake_score)
"""

from __future__ import annotations

import argparse
import json
from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Iterable, List, Tuple


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


@dataclass
class Row:
    segment: str
    label: int
    score: float


def iter_rows(path: Path, segment_field: str, label_field: str, score_field: str) -> Iterable[Row]:
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                payload = json.loads(line)
            except Exception:
                continue
            if not isinstance(payload, dict):
                continue

            seg = str(payload.get(segment_field, "*") or "*").strip() or "*"

            label_val = payload.get(label_field)
            try:
                label = int(label_val)
            except Exception:
                continue
            label = 1 if label > 0 else 0

            score = payload.get(score_field)
            if score is None:
                score = payload.get("spoof_prob")
            if score is None and payload.get("deepfake_score") is not None:
                try:
                    score = 1.0 - float(payload.get("deepfake_score"))
                except Exception:
                    score = None
            if score is None:
                continue

            try:
                score_f = clamp01(float(score))
            except Exception:
                continue

            yield Row(segment=seg, label=label, score=score_f)


def precision_recall(rows: List[Row], threshold: float) -> Tuple[float, float, int, int, int]:
    tp = fp = fn = 0
    for row in rows:
        pred = 1 if row.score >= threshold else 0
        if pred == 1 and row.label == 1:
            tp += 1
        elif pred == 1 and row.label == 0:
            fp += 1
        elif pred == 0 and row.label == 1:
            fn += 1

    precision = tp / (tp + fp) if (tp + fp) > 0 else 0.0
    recall = tp / (tp + fn) if (tp + fn) > 0 else 0.0
    return precision, recall, tp, fp, fn


def choose_threshold(rows: List[Row], target_precision: float, default_threshold: float) -> Tuple[float, Dict[str, float]]:
    if not rows:
        return default_threshold, {
            "precision": 0.0,
            "recall": 0.0,
            "tp": 0,
            "fp": 0,
            "fn": 0,
        }

    candidates = sorted({round(row.score, 6) for row in rows})
    if default_threshold not in candidates:
        candidates.append(default_threshold)
    candidates = sorted(set(candidates))

    best_threshold = default_threshold
    best_metrics = None

    for th in candidates:
        precision, recall, tp, fp, fn = precision_recall(rows, th)
        metrics = {
            "precision": precision,
            "recall": recall,
            "tp": tp,
            "fp": fp,
            "fn": fn,
        }

        if best_metrics is None:
            best_threshold = th
            best_metrics = metrics
            continue

        # Primary objective: satisfy precision target with max recall.
        if precision >= target_precision:
            if best_metrics["precision"] < target_precision or recall > best_metrics["recall"]:
                best_threshold = th
                best_metrics = metrics
            elif (
                best_metrics["precision"] >= target_precision
                and abs(recall - best_metrics["recall"]) < 1e-12
                and th > best_threshold
            ):
                # choose stricter threshold on tie
                best_threshold = th
                best_metrics = metrics
        else:
            # If no threshold meets target, keep highest precision then best recall.
            if best_metrics["precision"] < target_precision:
                if precision > best_metrics["precision"]:
                    best_threshold = th
                    best_metrics = metrics
                elif abs(precision - best_metrics["precision"]) < 1e-12 and recall > best_metrics["recall"]:
                    best_threshold = th
                    best_metrics = metrics

    assert best_metrics is not None
    return float(best_threshold), best_metrics


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-jsonl", required=True)
    parser.add_argument("--output-json", required=True)
    parser.add_argument("--segment-field", default="segment_key")
    parser.add_argument("--label-field", default="is_deepfake")
    parser.add_argument("--score-field", default="deepfake_prob")
    parser.add_argument("--min-samples-per-segment", type=int, default=30)
    parser.add_argument("--target-precision", type=float, default=0.90)
    parser.add_argument("--default-threshold", type=float, default=0.50)
    args = parser.parse_args()

    input_path = Path(args.input_jsonl).resolve()
    output_path = Path(args.output_json).resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)

    rows = list(iter_rows(
        path=input_path,
        segment_field=args.segment_field,
        label_field=args.label_field,
        score_field=args.score_field,
    ))

    by_segment: Dict[str, List[Row]] = defaultdict(list)
    for row in rows:
        by_segment[row.segment].append(row)
        by_segment["*"].append(row)

    thresholds: Dict[str, float] = {}
    metrics_out: Dict[str, Dict[str, float]] = {}

    for segment, seg_rows in by_segment.items():
        if len(seg_rows) < args.min_samples_per_segment and segment != "*":
            continue

        threshold, metrics = choose_threshold(
            seg_rows,
            target_precision=clamp01(args.target_precision),
            default_threshold=clamp01(args.default_threshold),
        )
        thresholds[segment] = round(clamp01(threshold), 6)
        metrics_out[segment] = {
            "samples": float(len(seg_rows)),
            "positives": float(sum(r.label for r in seg_rows)),
            "precision": round(clamp01(metrics["precision"]), 6),
            "recall": round(clamp01(metrics["recall"]), 6),
            "tp": float(metrics["tp"]),
            "fp": float(metrics["fp"]),
            "fn": float(metrics["fn"]),
        }

    if "*" not in thresholds:
        thresholds["*"] = round(clamp01(args.default_threshold), 6)
        metrics_out["*"] = {
            "samples": float(len(rows)),
            "positives": float(sum(r.label for r in rows)),
            "precision": 0.0,
            "recall": 0.0,
            "tp": 0.0,
            "fp": 0.0,
            "fn": 0.0,
        }

    payload = {
        "type": "deepfake_thresholds_v1",
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "source": str(input_path),
        "target_precision": round(clamp01(args.target_precision), 6),
        "default_threshold": round(clamp01(args.default_threshold), 6),
        "min_samples_per_segment": int(args.min_samples_per_segment),
        "thresholds": thresholds,
        "metrics": metrics_out,
    }

    output_path.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    print(json.dumps({"output_json": str(output_path), "segments": len(thresholds)}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
