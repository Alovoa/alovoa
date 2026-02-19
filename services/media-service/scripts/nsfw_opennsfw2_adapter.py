#!/usr/bin/env python3
"""
OpenNSFW2 adapter for media-service NSFW_EXTERNAL_SCORER_CMD.

Output contract (stdout JSON):
{
  "nsfw_score": 0.0..1.0,
  "confidence": 0.0..1.0,
  "categories": { "nsfw": float, "sfw": float },
  "signals": { ... }
}
"""

import argparse
import json
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def skin_proxy(image: np.ndarray) -> float:
    ycrcb = cv2.cvtColor(image, cv2.COLOR_BGR2YCrCb)
    mask = cv2.inRange(ycrcb, (0, 133, 77), (255, 173, 127))
    skin_ratio = float(np.mean(mask > 0))
    return clamp01((skin_ratio - 0.10) / 0.55)


def score_with_opennsfw2(image_path: Path) -> dict | None:
    try:
        import opennsfw2
    except Exception:
        return None

    image = cv2.imread(str(image_path))
    if image is None:
        return None
    rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    pil = Image.fromarray(rgb)

    try:
        nsfw_score = clamp01(float(opennsfw2.predict_image(pil)))
        return {
            "nsfw_score": nsfw_score,
            "confidence": 0.85,
            "categories": {
                "nsfw": nsfw_score,
                "sfw": round(1.0 - nsfw_score, 6),
            },
            "signals": {
                "provider_opennsfw2": 1.0,
            },
        }
    except Exception:
        return None


def score_fallback(image_path: Path) -> dict:
    image = cv2.imread(str(image_path))
    if image is None:
        return {
            "nsfw_score": 0.02,
            "confidence": 0.0,
            "categories": {"nsfw": 0.02, "sfw": 0.98},
            "signals": {"read_error": 1.0},
        }

    proxy = skin_proxy(image)
    nsfw_score = clamp01(0.05 + (0.70 * proxy))
    return {
        "nsfw_score": nsfw_score,
        "confidence": 0.30,
        "categories": {
            "nsfw": nsfw_score,
            "sfw": round(1.0 - nsfw_score, 6),
        },
        "signals": {
            "provider_heuristic": 1.0,
            "skin_proxy": round(proxy, 6),
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True)
    args = parser.parse_args()
    image_path = Path(args.image).resolve()

    payload = score_with_opennsfw2(image_path)
    if payload is None:
        payload = score_fallback(image_path)

    sys.stdout.write(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

