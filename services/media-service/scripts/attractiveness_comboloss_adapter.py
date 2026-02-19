#!/usr/bin/env python3
"""
ComboLoss attractiveness adapter.

Attempts to run inference from third_party ComboLoss checkout:
  https://github.com/lucasxlu/ComboLoss

Output contract:
{
  "score": 0.0..1.0,
  "confidence": 0.0..1.0,
  "signals": { ... }
}
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path
from typing import Any, Dict, Optional

import cv2
import numpy as np


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def round6(value: float) -> float:
    return round(float(value), 6)


def normalize_combo_beauty(raw: float) -> float:
    if raw <= 1.0:
        return clamp01(raw)
    if raw <= 5.0:
        return clamp01((raw - 1.0) / 4.0)
    if raw <= 10.0:
        return clamp01(raw / 10.0)
    return clamp01(1.0 - np.exp(-raw / 5.0))


def heuristic_score(image_path: Path) -> Dict[str, Any]:
    image = cv2.imread(str(image_path))
    if image is None or image.size == 0:
        return {
            "score": 0.5,
            "confidence": 0.0,
            "signals": {
                "provider_heuristic": 1.0,
                "read_error": 1.0,
            },
        }

    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    sharpness = clamp01(float(cv2.Laplacian(gray, cv2.CV_64F).var()) / 500.0)
    exposure = clamp01(1.0 - abs(float(np.mean(gray)) - 127.5) / 127.5)

    h, w = gray.shape[:2]
    if h > 8 and w > 8:
        mid = w // 2
        left = gray[:, :mid]
        right = gray[:, w - mid:]
        symmetry = clamp01(1.0 - (np.mean(np.abs(left.astype(np.float32) - cv2.flip(right, 1).astype(np.float32))) / 255.0))
    else:
        symmetry = 0.5

    score = clamp01((0.45 * sharpness) + (0.30 * exposure) + (0.25 * symmetry))
    return {
        "score": score,
        "confidence": 0.30,
        "signals": {
            "provider_heuristic": 1.0,
            "sharpness": round6(sharpness),
            "exposure": round6(exposure),
            "symmetry": round6(symmetry),
        },
    }


def run_comboloss(image_path: Path, repo_root: Path, model_path: Path) -> Optional[Dict[str, Any]]:
    try:
        repo_root = repo_root.resolve()
        model_path = model_path.resolve()
        if not repo_root.exists() or not model_path.exists():
            return None

        sys.path.insert(0, str(repo_root))
        from main.inference import FacialBeautyPredictor  # type: ignore

        predictor = FacialBeautyPredictor(pretrained_model_path=str(model_path))
        result = predictor.infer(str(image_path))

        raw = float(result.get("beauty", 0.0))
        elapsed = float(result.get("elapse", 0.0))
        score = normalize_combo_beauty(raw)
        confidence = 0.82
        if elapsed > 1.5:
            confidence = 0.75

        return {
            "score": score,
            "confidence": confidence,
            "signals": {
                "provider_comboloss": 1.0,
                "raw_beauty": round6(raw),
                "elapsed_sec": round6(elapsed),
                "model_loaded": 1.0,
            },
        }
    except Exception:
        return None


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True)
    parser.add_argument("--model", default=os.getenv("COMBOLOSS_MODEL_PATH", ""))
    parser.add_argument("--repo-root", default="")
    args = parser.parse_args()

    image_path = Path(args.image).resolve()
    repo_root = Path(args.repo_root).resolve() if args.repo_root else (
        Path(__file__).resolve().parents[3] / "third_party" / "repos" / "ComboLoss"
    )

    payload = None
    if args.model:
        payload = run_comboloss(image_path, repo_root, Path(args.model))
    if payload is None:
        payload = heuristic_score(image_path)
    sys.stdout.write(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
