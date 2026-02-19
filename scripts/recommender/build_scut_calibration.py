#!/usr/bin/env python3
"""
Build a score->percentile calibration file from SCUT-FBP5500 split metadata.

This script uses score labels in:
  third_party/repos/SCUT-FBP5500-Database-Release/data/*/(train_*.txt|test_*.txt)

Each line format:
  <image_name> <score_1_to_5>

Output JSON schema (score_percentile_v1):
{
  "type": "score_percentile_v1",
  "source": "SCUT-FBP5500",
  "n_samples": 5500,
  "quantiles": [{"q": 0.0, "score": 0.12}, ...]
}
"""

from __future__ import annotations

import argparse
import json
import math
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Tuple


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def parse_score_line(line: str) -> Tuple[str, float] | None:
    parts = line.strip().split()
    if len(parts) < 2:
        return None
    try:
        raw = float(parts[1])
    except Exception:
        return None

    image_name = str(parts[0]).strip()
    if not image_name:
        return None

    # SCUT labels are in [1, 5]; normalize to [0, 1].
    return image_name, clamp01((raw - 1.0) / 4.0)


def collect_scores(repo_root: Path) -> List[float]:
    data_root = repo_root / "data"
    if not data_root.exists():
        raise FileNotFoundError(f"SCUT data directory not found: {data_root}")

    by_image: Dict[str, float] = {}
    pattern_pairs = [
        "*/train_*.txt",
        "*/test_*.txt",
    ]
    for pattern in pattern_pairs:
        for file_path in sorted(data_root.glob(pattern)):
            try:
                lines = file_path.read_text(encoding="utf-8", errors="ignore").splitlines()
            except Exception:
                continue
            for line in lines:
                parsed = parse_score_line(line)
                if parsed is None:
                    continue
                image_name, score = parsed
                by_image[image_name] = score
    return list(by_image.values())


def build_quantiles(scores: List[float], quantile_count: int) -> List[Dict[str, float]]:
    quantile_count = max(2, int(quantile_count))
    sorted_scores = sorted(clamp01(s) for s in scores)
    n = len(sorted_scores)
    if n == 0:
        raise RuntimeError("Cannot build quantiles from empty score list")

    out: List[Dict[str, float]] = []
    for idx in range(quantile_count):
        q = 0.0 if quantile_count == 1 else float(idx) / float(quantile_count - 1)
        pos = q * float(n - 1)
        left = int(math.floor(pos))
        right = int(math.ceil(pos))
        if left == right:
            score = sorted_scores[left]
        else:
            t = pos - float(left)
            score = sorted_scores[left] * (1.0 - t) + sorted_scores[right] * t
        out.append({
            "q": round(q, 6),
            "score": round(clamp01(score), 6),
        })
    return out


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--repo-root",
        default=str(Path(__file__).resolve().parents[2] / "third_party" / "repos" / "SCUT-FBP5500-Database-Release"),
    )
    parser.add_argument("--output-json", required=True)
    parser.add_argument("--quantiles", type=int, default=201)
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    scores_list = collect_scores(repo_root)
    if not scores_list:
        raise RuntimeError(f"No scores found under {repo_root / 'data'}")

    quantiles = build_quantiles(scores_list, args.quantiles)
    n = len(scores_list)
    score_min = min(scores_list)
    score_max = max(scores_list)
    score_mean = sum(scores_list) / float(n)
    variance = sum((s - score_mean) * (s - score_mean) for s in scores_list) / float(n)
    score_std = math.sqrt(max(0.0, variance))

    payload = {
        "type": "score_percentile_v1",
        "source": "SCUT-FBP5500",
        "repo_root": repo_root.name,
        "created_at": datetime.now(timezone.utc).isoformat(),
        "n_samples": int(n),
        "normalized_min": round(clamp01(score_min), 6),
        "normalized_max": round(clamp01(score_max), 6),
        "normalized_mean": round(clamp01(score_mean), 6),
        "normalized_std": round(score_std, 6),
        "quantiles": quantiles,
    }

    output_path = Path(args.output_json).resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote SCUT calibration to {output_path} with {payload['n_samples']} samples")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
