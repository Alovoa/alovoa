#!/usr/bin/env python3
"""
Example external scorer adapter for media-service ATTRACTIVENESS_EXTERNAL_SCORER_CMD.

Usage:
  python external_scorer_example.py --image /tmp/in.jpg --view front

Output contract (stdout JSON):
  {
    "score": 0.0..1.0,
    "confidence": 0.0..1.0,
    "signals": { "...": 0.0..1.0 }
  }
"""

import argparse
import json
import sys

import cv2
import numpy as np


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def compute_score(image_path: str, view: str) -> dict:
    image = cv2.imread(image_path)
    if image is None:
        return {"score": 0.5, "confidence": 0.0, "signals": {"read_error": 1.0}}

    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    sharpness = clamp01(float(cv2.Laplacian(gray, cv2.CV_64F).var()) / 500.0)
    exposure = clamp01(1.0 - (abs(float(np.mean(gray)) - 127.5) / 127.5))

    # Placeholder scoring logic. Replace with ComboLoss/FaceAttract/MetaFBP inference.
    score = clamp01(0.55 * sharpness + 0.45 * exposure)
    confidence = clamp01(0.40 + 0.60 * sharpness)

    return {
        "score": score,
        "confidence": confidence,
        "signals": {
            "view_front": 1.0 if view == "front" else 0.0,
            "view_side": 1.0 if view == "side" else 0.0,
            "sharpness": sharpness,
            "exposure": exposure,
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True)
    parser.add_argument("--view", default="front")
    args = parser.parse_args()

    payload = compute_score(args.image, args.view)
    sys.stdout.write(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
