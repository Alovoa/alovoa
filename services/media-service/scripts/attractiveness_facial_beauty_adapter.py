#!/usr/bin/env python3
"""
Facial-beauty-prediction adapter.

Attempts to run prediction from third_party checkout:
  https://github.com/etrain-xyz/facial-beauty-prediction

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
import re
import subprocess
import sys
from pathlib import Path
from typing import Any, Dict, Optional

import cv2
import numpy as np


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def round6(value: float) -> float:
    return round(float(value), 6)


def normalize_beauty(raw: float) -> float:
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
    score = clamp01((0.55 * sharpness) + (0.45 * exposure))

    return {
        "score": score,
        "confidence": 0.30,
        "signals": {
            "provider_heuristic": 1.0,
            "sharpness": round6(sharpness),
            "exposure": round6(exposure),
        },
    }


def run_repo_predictor(
    image_path: Path,
    model_path: Path,
    repo_root: Path,
    python_bin: str,
    timeout_sec: int,
) -> Optional[Dict[str, Any]]:
    predict_py = repo_root / "predict.py"
    if not predict_py.exists() or not model_path.exists():
        return None

    try:
        proc = subprocess.run(
            [python_bin, str(predict_py), "-i", str(image_path), "-m", str(model_path)],
            cwd=str(repo_root),
            capture_output=True,
            text=True,
            timeout=max(1, timeout_sec),
            check=False,
        )
        output = f"{proc.stdout}\n{proc.stderr}"
        match = re.search(r"output\s+([0-9]+(?:\.[0-9]+)?)", output)
        if not match:
            return None

        raw = float(match.group(1))
        score = normalize_beauty(raw)
        confidence = 0.68 if proc.returncode == 0 else 0.55

        return {
            "score": score,
            "confidence": confidence,
            "signals": {
                "provider_facial_beauty_prediction": 1.0,
                "raw_beauty": round6(raw),
                "process_exit_code": float(proc.returncode),
            },
        }
    except Exception:
        return None


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True)
    parser.add_argument("--model", default=os.getenv("FACIAL_BEAUTY_MODEL_PATH", ""))
    parser.add_argument("--repo-root", default="")
    parser.add_argument("--python-bin", default=os.getenv("FACIAL_BEAUTY_PYTHON_BIN", sys.executable))
    parser.add_argument("--timeout-sec", type=int, default=int(os.getenv("FACIAL_BEAUTY_TIMEOUT_SEC", "12")))
    args = parser.parse_args()

    image_path = Path(args.image).resolve()
    repo_root = Path(args.repo_root).resolve() if args.repo_root else (
        Path(__file__).resolve().parents[3] / "third_party" / "repos" / "facial-beauty-prediction"
    )

    payload = None
    if args.model:
        payload = run_repo_predictor(
            image_path=image_path,
            model_path=Path(args.model).resolve(),
            repo_root=repo_root,
            python_bin=args.python_bin,
            timeout_sec=args.timeout_sec,
        )
    if payload is None:
        payload = heuristic_score(image_path)

    sys.stdout.write(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
