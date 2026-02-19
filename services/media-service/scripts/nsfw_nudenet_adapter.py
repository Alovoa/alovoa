#!/usr/bin/env python3
"""
NudeNet adapter for media-service NSFW_EXTERNAL_SCORER_CMD.

Output contract (stdout JSON):
{
  "nsfw_score": 0.0..1.0,
  "confidence": 0.0..1.0,
  "categories": { ... },
  "signals": { ... }
}
"""

import argparse
import json
import sys
from pathlib import Path

import cv2


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


EXPLICIT_LABELS = {
    "FEMALE_GENITALIA_EXPOSED",
    "MALE_GENITALIA_EXPOSED",
    "ANUS_EXPOSED",
    "BUTTOCKS_EXPOSED",
    "FEMALE_BREAST_EXPOSED",
}

IMPLIED_LABELS = {
    "BELLY_EXPOSED",
    "ARMPITS_EXPOSED",
    "MALE_BREAST_EXPOSED",
}


def score_with_nudenet(image_path: Path) -> dict | None:
    try:
        from nudenet import NudeDetector
    except Exception:
        return None

    image = cv2.imread(str(image_path))
    if image is None:
        return None

    try:
        detector = NudeDetector()
        detections = detector.detect(image) or []
    except Exception:
        return None

    categories = {}
    for det in detections:
        if not isinstance(det, dict):
            continue
        cls = str(det.get("class", "")).strip()
        if not cls:
            continue
        score = clamp01(float(det.get("score", 0.0)))
        categories[cls] = max(categories.get(cls, 0.0), score)

    explicit = max([categories.get(k, 0.0) for k in EXPLICIT_LABELS], default=0.0)
    implied = max([categories.get(k, 0.0) for k in IMPLIED_LABELS], default=0.0)
    nsfw_score = clamp01(max(explicit, implied * 0.55))
    confidence = clamp01(max(categories.values(), default=0.0))

    json_categories = {k.lower(): round(v, 6) for k, v in categories.items()}
    json_categories["nsfw"] = round(nsfw_score, 6)
    json_categories["sfw"] = round(1.0 - nsfw_score, 6)

    return {
        "nsfw_score": nsfw_score,
        "confidence": confidence,
        "categories": json_categories,
        "signals": {
            "provider_nudenet": 1.0,
            "detections": float(len(detections)),
        },
    }


def score_fallback() -> dict:
    return {
        "nsfw_score": 0.02,
        "confidence": 0.0,
        "categories": {"nsfw": 0.02, "sfw": 0.98},
        "signals": {"provider_fallback": 1.0},
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True)
    args = parser.parse_args()
    image_path = Path(args.image).resolve()

    payload = score_with_nudenet(image_path)
    if payload is None:
        payload = score_fallback()

    sys.stdout.write(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

