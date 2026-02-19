#!/usr/bin/env python3
"""
InsightFace attractiveness geometry adapter.

Attempts to run:
  https://github.com/deepinsight/insightface

Output contract:
{
  "score": 0.0..1.0,
  "confidence": 0.0..1.0,
  "provider": "insightface_geometry",
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
    from insightface.app import FaceAnalysis
    INSIGHTFACE_AVAILABLE = True
except Exception:
    FaceAnalysis = None
    INSIGHTFACE_AVAILABLE = False

_APP = None
_FAILED = False


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def round6(value: float) -> float:
    return round(float(value), 6)


def normalize_sharpness(gray: np.ndarray) -> float:
    return clamp01(float(cv2.Laplacian(gray, cv2.CV_64F).var()) / 500.0)


def normalize_exposure(gray: np.ndarray) -> float:
    return clamp01(1.0 - (abs(float(np.mean(gray)) - 127.5) / 127.5))


def get_app():
    global _APP, _FAILED
    if _FAILED or not INSIGHTFACE_AVAILABLE:
        return None
    if _APP is not None:
        return _APP

    try:
        model_name = os.getenv("INSIGHTFACE_MODEL_NAME", "buffalo_l")
        provider_env = os.getenv("INSIGHTFACE_PROVIDERS", "CPUExecutionProvider")
        providers = [p.strip() for p in provider_env.split(",") if p.strip()]
        ctx_id = int(os.getenv("INSIGHTFACE_CTX_ID", "-1"))
        det_size = int(os.getenv("INSIGHTFACE_DET_SIZE", "640"))
        app = FaceAnalysis(name=model_name, providers=providers)
        app.prepare(ctx_id=ctx_id, det_size=(det_size, det_size))
        _APP = app
    except Exception:
        _FAILED = True
        _APP = None
    return _APP


def run_insightface(image: np.ndarray) -> Optional[Dict[str, Any]]:
    app = get_app()
    if app is None:
        return None

    try:
        faces = app.get(image)
        if not faces:
            return None
        face = max(faces, key=lambda f: float(getattr(f, "det_score", 0.0)))
    except Exception:
        return None

    h, w = image.shape[:2]
    bbox = getattr(face, "bbox", None)
    if bbox is None or len(bbox) < 4:
        return None

    x1, y1, x2, y2 = [float(v) for v in bbox[:4]]
    face_w = max(0.0, x2 - x1)
    face_h = max(0.0, y2 - y1)
    fill = clamp01((face_w * face_h) / max(1.0, float(w * h)))

    pose = getattr(face, "pose", None)
    yaw = 0.5
    pitch = 0.5
    if pose is not None and len(pose) >= 2:
        pitch = clamp01(abs(float(pose[0])) / 45.0)
        yaw = clamp01(abs(float(pose[1])) / 45.0)

    framing = clamp01(1.0 - abs(fill - 0.30) / 0.30)
    pose_front = clamp01(1.0 - (0.7 * yaw + 0.3 * pitch))
    det = clamp01(float(getattr(face, "det_score", 0.0)))

    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    sharpness = normalize_sharpness(gray)
    exposure = normalize_exposure(gray)

    score = clamp01(
        (0.22 * det)
        + (0.18 * framing)
        + (0.20 * pose_front)
        + (0.20 * sharpness)
        + (0.20 * exposure)
    )
    confidence = clamp01(0.55 + (0.35 * det) + (0.10 * pose_front))

    return {
        "score": round6(score),
        "confidence": round6(confidence),
        "signals": {
            "provider_insightface_geometry": 1.0,
            "insight_det": round6(det),
            "insight_fill": round6(fill),
            "insight_yaw": round6(yaw),
            "insight_pitch": round6(pitch),
            "framing": round6(framing),
            "pose_front": round6(pose_front),
            "sharpness": round6(sharpness),
            "exposure": round6(exposure),
            "face_count_norm": round6(clamp01(len(faces) / 3.0)),
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
    parser.add_argument("--model-version", default=os.getenv("INSIGHTFACE_MODEL_VERSION", "insightface_geometry_v1"))
    args = parser.parse_args()

    image_path = Path(args.image).resolve()
    image = cv2.imread(str(image_path))
    if image is None or image.size == 0:
        payload = {
            "score": 0.5,
            "confidence": 0.0,
            "provider": "insightface_geometry",
            "model_version": args.model_version,
            "signals": {
                "provider_heuristic": 1.0,
                "read_error": 1.0,
            },
        }
        print(json.dumps(payload))
        return 0

    scored = run_insightface(image)
    if scored is None:
        scored = heuristic(image)

    payload = {
        "score": float(scored["score"]),
        "confidence": float(scored["confidence"]),
        "provider": "insightface_geometry",
        "model_version": args.model_version,
        "signals": scored["signals"],
    }
    print(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
