#!/usr/bin/env python3
"""
MediaPipe geometry adapter.

Attempts to run:
  https://github.com/google-ai-edge/mediapipe

Output contract:
{
  "score": 0.0..1.0,
  "confidence": 0.0..1.0,
  "provider": "mediapipe_geometry",
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
    import mediapipe as mp  # type: ignore
    MEDIAPIPE_AVAILABLE = True
except Exception:
    mp = None
    MEDIAPIPE_AVAILABLE = False


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def round6(value: float) -> float:
    return round(float(value), 6)


def target_closeness(value: float, target: float, tolerance: float) -> float:
    return clamp01(1.0 - abs(value - target) / max(tolerance, 1e-6))


def mediapipe_geometry(image: np.ndarray) -> Optional[Dict[str, Any]]:
    if not MEDIAPIPE_AVAILABLE:
        return None

    rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    try:
        mesh = mp.solutions.face_mesh.FaceMesh(
            static_image_mode=True,
            max_num_faces=1,
            refine_landmarks=True,
            min_detection_confidence=0.5,
        )
        result = mesh.process(rgb)
        mesh.close()
    except Exception:
        return None

    if not result.multi_face_landmarks:
        return None

    landmarks = result.multi_face_landmarks[0].landmark

    def p(idx: int) -> np.ndarray:
        lm = landmarks[idx]
        return np.array([lm.x, lm.y, lm.z], dtype=np.float32)

    center = p(1)
    sym_pairs = [(33, 263), (61, 291), (159, 386), (145, 374), (93, 323), (132, 361), (58, 288)]
    asym = []
    for left_idx, right_idx in sym_pairs:
        left = p(left_idx)
        right = p(right_idx)
        dl = float(np.linalg.norm(left[:2] - center[:2]))
        dr = float(np.linalg.norm(right[:2] - center[:2]))
        asym.append(abs(dl - dr) / max(dl, dr, 1e-6))
    symmetry = clamp01(1.0 - float(np.mean(asym))) if asym else 0.5

    face_w = float(np.linalg.norm(p(234)[:2] - p(454)[:2]))
    face_h = float(np.linalg.norm(p(10)[:2] - p(152)[:2]))
    eye_w = float(np.linalg.norm(p(33)[:2] - p(263)[:2]))
    mouth_w = float(np.linalg.norm(p(61)[:2] - p(291)[:2]))

    if min(face_w, face_h, eye_w, mouth_w) <= 1e-6:
        return None

    ratio = face_w / face_h
    eye_mouth = eye_w / mouth_w
    ratio_score = target_closeness(ratio, 0.82, 0.45)
    eye_mouth_score = target_closeness(eye_mouth, 0.75, 0.50)

    depth_span = float(abs(p(1)[2] - p(152)[2]) + abs(p(234)[2] - p(454)[2]))
    depth_score = clamp01(1.0 - (depth_span / 0.25))

    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    sharpness = clamp01(float(cv2.Laplacian(gray, cv2.CV_64F).var()) / 500.0)
    exposure = clamp01(1.0 - abs(float(np.mean(gray)) - 127.5) / 127.5)

    score = clamp01(
        (0.30 * symmetry)
        + (0.18 * ratio_score)
        + (0.16 * eye_mouth_score)
        + (0.12 * depth_score)
        + (0.12 * sharpness)
        + (0.12 * exposure)
    )
    confidence = clamp01(0.62 + (0.20 * symmetry) + (0.10 * depth_score))

    return {
        "score": round6(score),
        "confidence": round6(confidence),
        "signals": {
            "provider_mediapipe_geometry": 1.0,
            "symmetry_score": round6(symmetry),
            "face_ratio": round6(ratio),
            "face_ratio_score": round6(ratio_score),
            "eye_mouth_ratio": round6(eye_mouth),
            "eye_mouth_score": round6(eye_mouth_score),
            "depth_span": round6(depth_span),
            "depth_score": round6(depth_score),
            "sharpness": round6(sharpness),
            "exposure": round6(exposure),
        },
    }


def heuristic(image: np.ndarray) -> Dict[str, Any]:
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    sharpness = clamp01(float(cv2.Laplacian(gray, cv2.CV_64F).var()) / 500.0)
    exposure = clamp01(1.0 - abs(float(np.mean(gray)) - 127.5) / 127.5)

    h, w = gray.shape[:2]
    symmetry = 0.5
    if h > 8 and w > 8:
        mid = w // 2
        if mid > 4 and (w - mid) > 4:
            left = gray[:, :mid]
            right = gray[:, w - mid:]
            right_flip = cv2.flip(right, 1)
            diff = float(np.mean(np.abs(left.astype(np.float32) - right_flip.astype(np.float32)))) / 255.0
            symmetry = clamp01(1.0 - diff)

    score = clamp01((0.40 * sharpness) + (0.35 * exposure) + (0.25 * symmetry))
    return {
        "score": round6(score),
        "confidence": 0.30,
        "signals": {
            "provider_heuristic": 1.0,
            "sharpness": round6(sharpness),
            "exposure": round6(exposure),
            "symmetry": round6(symmetry),
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True)
    parser.add_argument("--model-version", default=os.getenv("MEDIAPIPE_MODEL_VERSION", "mediapipe_geometry_v1"))
    args = parser.parse_args()

    image_path = Path(args.image).resolve()
    image = cv2.imread(str(image_path))
    if image is None or image.size == 0:
        payload = {
            "score": 0.5,
            "confidence": 0.0,
            "provider": "mediapipe_geometry",
            "model_version": args.model_version,
            "signals": {
                "provider_heuristic": 1.0,
                "read_error": 1.0,
            },
        }
        print(json.dumps(payload))
        return 0

    scored = mediapipe_geometry(image)
    if scored is None:
        scored = heuristic(image)

    payload = {
        "score": float(scored["score"]),
        "confidence": float(scored["confidence"]),
        "provider": "mediapipe_geometry",
        "model_version": args.model_version,
        "signals": scored["signals"],
    }
    print(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
