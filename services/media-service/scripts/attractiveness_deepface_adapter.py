#!/usr/bin/env python3
"""
DeepFace quality adapter.

Attempts to run:
  https://github.com/serengil/deepface

Output contract:
{
  "score": 0.0..1.0,
  "confidence": 0.0..1.0,
  "provider": "deepface_quality",
  "model_version": "...",
  "signals": { ... }
}
"""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
from typing import Any, Dict, Optional

import cv2
import numpy as np

try:
    from deepface import DeepFace  # type: ignore
    DEEPFACE_AVAILABLE = True
except Exception:
    DeepFace = None
    DEEPFACE_AVAILABLE = False


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def round6(value: float) -> float:
    return round(float(value), 6)


def normalize_sharpness(gray: np.ndarray) -> float:
    return clamp01(float(cv2.Laplacian(gray, cv2.CV_64F).var()) / 500.0)


def normalize_exposure(gray: np.ndarray) -> float:
    return clamp01(1.0 - abs(float(np.mean(gray)) - 127.5) / 127.5)


def run_deepface(image_path: Path) -> Optional[Dict[str, Any]]:
    if not DEEPFACE_AVAILABLE:
        return None

    try:
        faces = DeepFace.extract_faces(
            img_path=str(image_path),
            detector_backend=os.getenv("DEEPFACE_DETECTOR_BACKEND", "opencv"),
            enforce_detection=False,
            align=True,
            anti_spoofing=False,
        )
    except Exception:
        return None

    if not faces:
        return None

    face = max(faces, key=lambda f: float(f.get("confidence", 0.0)))
    confidence_raw = clamp01(float(face.get("confidence", 0.0)))

    facial_area = face.get("facial_area", {}) if isinstance(face, dict) else {}
    area_ratio = 0.5
    if isinstance(facial_area, dict):
        w = float(max(1, facial_area.get("w", 1)))
        h = float(max(1, facial_area.get("h", 1)))
        img = cv2.imread(str(image_path))
        if img is not None and img.size > 0:
            ih, iw = img.shape[:2]
            area_ratio = clamp01((w * h) / max(1.0, float(iw * ih)))

    image = cv2.imread(str(image_path))
    if image is None or image.size == 0:
        return None
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    sharpness = normalize_sharpness(gray)
    exposure = normalize_exposure(gray)

    framing = clamp01(1.0 - abs(area_ratio - 0.30) / 0.30)
    score = clamp01(
        (0.28 * confidence_raw)
        + (0.22 * framing)
        + (0.25 * sharpness)
        + (0.25 * exposure)
    )
    confidence = clamp01(0.52 + (0.38 * confidence_raw))

    return {
        "score": round6(score),
        "confidence": round6(confidence),
        "signals": {
            "provider_deepface_quality": 1.0,
            "deepface_detection_confidence": round6(confidence_raw),
            "face_area_ratio": round6(area_ratio),
            "framing": round6(framing),
            "sharpness": round6(sharpness),
            "exposure": round6(exposure),
            "detected_faces_norm": round6(clamp01(len(faces) / 3.0)),
        },
    }


def heuristic(image: np.ndarray) -> Dict[str, Any]:
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    sharpness = normalize_sharpness(gray)
    exposure = normalize_exposure(gray)
    score = clamp01((0.5 * sharpness) + (0.5 * exposure))
    return {
        "score": round6(score),
        "confidence": 0.30,
        "signals": {
            "provider_heuristic": 1.0,
            "sharpness": round6(sharpness),
            "exposure": round6(exposure),
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True)
    parser.add_argument("--model-version", default=os.getenv("DEEPFACE_MODEL_VERSION", "deepface_quality_v1"))
    args = parser.parse_args()

    image_path = Path(args.image).resolve()
    image = cv2.imread(str(image_path))
    if image is None or image.size == 0:
        payload = {
            "score": 0.5,
            "confidence": 0.0,
            "provider": "deepface_quality",
            "model_version": args.model_version,
            "signals": {
                "provider_heuristic": 1.0,
                "read_error": 1.0,
            },
        }
        print(json.dumps(payload))
        return 0

    scored = run_deepface(image_path)
    if scored is None:
        scored = heuristic(image)

    payload = {
        "score": float(scored["score"]),
        "confidence": float(scored["confidence"]),
        "provider": "deepface_quality",
        "model_version": args.model_version,
        "signals": scored["signals"],
    }
    print(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
