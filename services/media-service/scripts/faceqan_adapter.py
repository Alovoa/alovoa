#!/usr/bin/env python3
"""
FaceQAN-inspired face quality adapter.

Primary mode: fast local quality features (face framing, sharpness, exposure, symmetry proxy).
Optional mode: run upstream FaceQAN command via FACEQAN_UPSTREAM_CMD and blend scores.

Output contract:
{
  "quality_score": 0.0..1.0,
  "confidence": 0.0..1.0,
  "provider": "faceqan",
  "model_version": "faceqan_v1",
  "signals": {...}
}
"""

from __future__ import annotations

import argparse
import json
import os
import shlex
import subprocess
from pathlib import Path
from typing import Dict, Optional

import cv2
import numpy as np

try:
    import mediapipe as mp  # type: ignore
    MEDIAPIPE_AVAILABLE = True
except Exception:
    mp = None
    MEDIAPIPE_AVAILABLE = False


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def round6(value: float) -> float:
    return round(float(value), 6)


def normalize_sharpness(gray: np.ndarray) -> float:
    return clamp01(float(cv2.Laplacian(gray, cv2.CV_64F).var()) / 450.0)


def normalize_exposure(gray: np.ndarray) -> float:
    return clamp01(1.0 - abs(float(np.mean(gray)) - 127.5) / 127.5)


def detect_face_ratio(image: np.ndarray) -> float:
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
    faces = cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=4, minSize=(64, 64))
    if len(faces) == 0:
        return 0.0
    x, y, w, h = max(faces, key=lambda r: int(r[2]) * int(r[3]))
    ih, iw = image.shape[:2]
    return clamp01((float(w) * float(h)) / max(1.0, float(iw * ih)))


def symmetry_proxy(image: np.ndarray) -> float:
    if not MEDIAPIPE_AVAILABLE:
        return 0.5

    rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    with mp.solutions.face_mesh.FaceMesh(  # type: ignore[attr-defined]
        static_image_mode=True,
        max_num_faces=1,
        refine_landmarks=False,
        min_detection_confidence=0.5,
    ) as mesh:
        result = mesh.process(rgb)

    if not result.multi_face_landmarks:
        return 0.5

    lm = result.multi_face_landmarks[0].landmark
    # landmark ids near left/right eyes and nose tip
    left_eye = lm[33]
    right_eye = lm[263]
    nose_tip = lm[1]

    left_dist = abs(nose_tip.x - left_eye.x)
    right_dist = abs(right_eye.x - nose_tip.x)
    if max(left_dist, right_dist) <= 1e-6:
        return 0.5

    asym = abs(left_dist - right_dist) / max(left_dist, right_dist)
    return clamp01(1.0 - asym)


def upstream_score(image_path: Path) -> Optional[float]:
    template = os.getenv("FACEQAN_UPSTREAM_CMD", "").strip()
    if not template:
        return None

    command = template.replace("{image_path}", shlex.quote(str(image_path)))
    try:
        completed = subprocess.run(
            command,
            shell=True,
            check=True,
            text=True,
            timeout=int(os.getenv("FACEQAN_UPSTREAM_TIMEOUT_SEC", "25")),
            capture_output=True,
        )
    except Exception:
        return None

    raw = (completed.stdout or "").strip()
    if not raw:
        return None

    try:
        payload = json.loads(raw)
        if isinstance(payload, dict):
            if "quality_score" in payload:
                return clamp01(float(payload["quality_score"]))
            if "score" in payload:
                return clamp01(float(payload["score"]))
        if isinstance(payload, (int, float)):
            return clamp01(float(payload))
    except Exception:
        pass

    try:
        return clamp01(float(raw))
    except Exception:
        return None


def score(image_path: Path) -> Dict[str, float]:
    image = cv2.imread(str(image_path))
    if image is None or image.size == 0:
        return {
            "quality_score": 0.5,
            "confidence": 0.0,
            "provider": "faceqan",
            "model_version": "faceqan_v1",
            "signals": {
                "read_error": 1.0,
            },
        }

    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    sharpness = normalize_sharpness(gray)
    exposure = normalize_exposure(gray)
    face_ratio = detect_face_ratio(image)
    framing = clamp01(1.0 - abs(face_ratio - 0.28) / 0.28) if face_ratio > 0 else 0.0
    symmetry = symmetry_proxy(image)

    local_score = clamp01(
        (0.32 * sharpness)
        + (0.22 * exposure)
        + (0.24 * framing)
        + (0.22 * symmetry)
    )

    quality = local_score
    used_upstream = 0.0
    upstream = upstream_score(image_path)
    if upstream is not None:
        quality = clamp01(0.55 * local_score + 0.45 * upstream)
        used_upstream = 1.0

    confidence = clamp01(0.45 + 0.45 * face_ratio + 0.10 * symmetry)

    return {
        "quality_score": round6(quality),
        "confidence": round6(confidence),
        "provider": "faceqan",
        "model_version": "faceqan_v1",
        "signals": {
            "provider_faceqan": 1.0,
            "sharpness": round6(sharpness),
            "exposure": round6(exposure),
            "face_ratio": round6(face_ratio),
            "framing": round6(framing),
            "symmetry": round6(symmetry),
            "upstream_used": used_upstream,
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True)
    args = parser.parse_args()

    payload = score(Path(args.image).resolve())
    print(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
