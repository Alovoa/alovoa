#!/usr/bin/env python3
"""
Build NSFW threshold calibration by segment from labeled score data.

Input format: JSONL where each line contains at least:
  - nsfw_score: float in [0,1]
  - label: one of {0,1,false,true,"safe","nsfw","allow","block"}
Optional:
  - segment_key: string (defaults to "*")

The calibration chooses threshold per segment that maximizes balanced accuracy.
"""

from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Iterable, List, Tuple


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def parse_label(value) -> int | None:
    if isinstance(value, bool):
        return 1 if value else 0
    if isinstance(value, (int, float)):
        if value >= 1:
            return 1
        if value <= 0:
            return 0
    text = str(value).strip().lower()
    if text in {"1", "true", "nsfw", "block", "unsafe"}:
        return 1
    if text in {"0", "false", "safe", "allow", "sfw"}:
        return 0
    return None


@dataclass
class Sample:
    score: float
    label: int  # 1 = unsafe, 0 = safe


def read_jsonl(path: Path) -> Dict[str, List[Sample]]:
    by_segment: Dict[str, List[Sample]] = {}
    for line in path.read_text(encoding="utf-8", errors="ignore").splitlines():
        line = line.strip()
        if not line:
            continue
        try:
            payload = json.loads(line)
        except Exception:
            continue
        if not isinstance(payload, dict):
            continue

        score_raw = payload.get("nsfw_score")
        label_raw = payload.get("label")
        if score_raw is None or label_raw is None:
            continue
        try:
            score = clamp01(float(score_raw))
        except Exception:
            continue
        label = parse_label(label_raw)
        if label is None:
            continue

        segment = str(payload.get("segment_key", "*")).strip() or "*"
        by_segment.setdefault(segment, []).append(Sample(score=score, label=label))
    return by_segment


def confusion(samples: Iterable[Sample], threshold: float) -> Tuple[int, int, int, int]:
    tp = fp = tn = fn = 0
    for s in samples:
        pred = 1 if s.score >= threshold else 0
        if pred == 1 and s.label == 1:
            tp += 1
        elif pred == 1 and s.label == 0:
            fp += 1
        elif pred == 0 and s.label == 0:
            tn += 1
        else:
            fn += 1
    return tp, fp, tn, fn


def balanced_accuracy(tp: int, fp: int, tn: int, fn: int) -> float:
    tpr = tp / (tp + fn) if (tp + fn) > 0 else 0.0
    tnr = tn / (tn + fp) if (tn + fp) > 0 else 0.0
    return 0.5 * (tpr + tnr)


def choose_threshold(samples: List[Sample], default_threshold: float) -> Tuple[float, Dict[str, float]]:
    if not samples:
        return default_threshold, {
            "samples": 0,
            "balanced_accuracy": 0.0,
            "precision": 0.0,
            "recall": 0.0,
        }

    candidates = sorted({round(s.score, 6) for s in samples})
    candidates = [0.0] + candidates + [1.0]
    best_threshold = default_threshold
    best_metrics = {
        "balanced_accuracy": -1.0,
        "precision": 0.0,
        "recall": 0.0,
    }

    for threshold in candidates:
        tp, fp, tn, fn = confusion(samples, threshold)
        ba = balanced_accuracy(tp, fp, tn, fn)
        precision = tp / (tp + fp) if (tp + fp) > 0 else 0.0
        recall = tp / (tp + fn) if (tp + fn) > 0 else 0.0

        if ba > best_metrics["balanced_accuracy"] + 1e-12:
            best_threshold = threshold
            best_metrics = {
                "balanced_accuracy": ba,
                "precision": precision,
                "recall": recall,
            }

    return clamp01(best_threshold), {
        "samples": float(len(samples)),
        "balanced_accuracy": round(best_metrics["balanced_accuracy"], 6),
        "precision": round(best_metrics["precision"], 6),
        "recall": round(best_metrics["recall"], 6),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-jsonl", required=True)
    parser.add_argument("--output-json", required=True)
    parser.add_argument("--default-threshold", type=float, default=0.60)
    parser.add_argument("--min-samples-per-segment", type=int, default=30)
    args = parser.parse_args()

    input_path = Path(args.input_jsonl).resolve()
    by_segment = read_jsonl(input_path)
    if not by_segment:
        raise RuntimeError(f"No valid labeled samples found in {input_path}")

    min_samples = max(1, int(args.min_samples_per_segment))
    default_threshold = clamp01(float(args.default_threshold))

    thresholds: Dict[str, float] = {}
    stats: Dict[str, Dict[str, float]] = {}

    for segment, samples in sorted(by_segment.items()):
        if len(samples) < min_samples:
            continue
        threshold, segment_stats = choose_threshold(samples, default_threshold)
        thresholds[segment] = round(threshold, 6)
        stats[segment] = segment_stats

    # Ensure global fallback.
    if "*" not in thresholds:
        all_samples: List[Sample] = []
        for samples in by_segment.values():
            all_samples.extend(samples)
        threshold, segment_stats = choose_threshold(all_samples, default_threshold)
        thresholds["*"] = round(threshold, 6)
        stats["*"] = segment_stats

    payload = {
        "type": "nsfw_thresholds_v1",
        "source": str(input_path.name),
        "created_at": datetime.now(timezone.utc).isoformat(),
        "min_samples_per_segment": min_samples,
        "default_threshold": round(default_threshold, 6),
        "thresholds": thresholds,
        "stats": stats,
    }

    output_path = Path(args.output_json).resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote NSFW calibration to {output_path} for {len(thresholds)} segments")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
