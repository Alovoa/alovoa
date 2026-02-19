#!/usr/bin/env python3
"""
face-alignment geometry adapter.

Attempts to run:
  https://github.com/1adrianb/face-alignment

The adapter extracts 2D landmark geometry signals and maps them into a bounded
attractiveness prior.

Output contract:
{
  "score": 0.0..1.0,
  "confidence": 0.0..1.0,
  "provider": "face_alignment_geometry",
  "model_version": "...",
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


def target_closeness(value: float, target: float, tolerance: float) -> float:
    return clamp01(1.0 - (abs(value - target) / max(tolerance, 1e-6)))


def import_face_alignment(repo_root: Path):
    try:
        import face_alignment  # type: ignore
        return face_alignment
    except Exception:
        pass

    try:
        if repo_root.exists():
            sys.path.insert(0, str(repo_root))
        import face_alignment  # type: ignore
        return face_alignment
    except Exception:
        return None


def geometry_from_landmarks(lm: np.ndarray) -> Optional[Dict[str, float]]:
    if lm.ndim != 2 or lm.shape[0] < 68 or lm.shape[1] < 2:
        return None

    def dist(i: int, j: int) -> float:
        return float(np.linalg.norm(lm[i, :2] - lm[j, :2]))

    center_x = float(lm[30, 0])
    pairs = [(1, 15), (2, 14), (3, 13), (4, 12), (5, 11), (6, 10), (7, 9)]
    pair_diffs = []
    for left_idx, right_idx in pairs:
        dl = abs(float(lm[left_idx, 0]) - center_x)
        dr = abs(float(lm[right_idx, 0]) - center_x)
        denom = max(dl, dr, 1e-6)
        pair_diffs.append(abs(dl - dr) / denom)
    symmetry = clamp01(1.0 - float(np.mean(pair_diffs))) if pair_diffs else 0.5

    jaw_width = dist(0, 16)
    face_height = dist(27, 8)
    interocular = dist(36, 45)
    mouth_width = dist(48, 54)
    brow_to_nose = dist(27, 33)
    nose_to_chin = dist(33, 8)

    if min(jaw_width, face_height, interocular, mouth_width, brow_to_nose, nose_to_chin) <= 1e-6:
        return None

    jaw_ratio = jaw_width / face_height
    eye_mouth_ratio = interocular / mouth_width
    upper_lower_ratio = brow_to_nose / nose_to_chin

    jaw_ratio_score = target_closeness(jaw_ratio, 0.90, 0.60)
    eye_mouth_score = target_closeness(eye_mouth_ratio, 0.75, 0.50)
    upper_lower_score = target_closeness(upper_lower_ratio, 0.95, 0.55)

    # Penalize obvious rotation; this is a weak 2D proxy for frontal quality.
    eye_line_angle = abs(float(np.arctan2(lm[45, 1] - lm[36, 1], lm[45, 0] - lm[36, 0])))
    pose_score = clamp01(1.0 - (eye_line_angle / 0.70))

    score = clamp01(
        (0.34 * symmetry)
        + (0.20 * jaw_ratio_score)
        + (0.18 * eye_mouth_score)
        + (0.18 * upper_lower_score)
        + (0.10 * pose_score)
    )

    return {
        "score": score,
        "symmetry_score": symmetry,
        "jaw_ratio": jaw_ratio,
        "jaw_ratio_score": jaw_ratio_score,
        "eye_mouth_ratio": eye_mouth_ratio,
        "eye_mouth_score": eye_mouth_score,
        "upper_lower_ratio": upper_lower_ratio,
        "upper_lower_score": upper_lower_score,
        "pose_score": pose_score,
    }


def run_face_alignment(
    image_path: Path,
    repo_root: Path,
    model_version: str,
) -> Optional[Dict[str, Any]]:
    face_alignment = import_face_alignment(repo_root)
    if face_alignment is None:
        return None

    image = cv2.imread(str(image_path))
    if image is None or image.size == 0:
        return None

    rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    device = os.getenv("FACE_ALIGNMENT_DEVICE", "cpu")

    try:
        aligner = face_alignment.FaceAlignment(
            face_alignment.LandmarksType.TWO_D,
            flip_input=False,
            device=device,
        )
    except Exception:
        return None

    try:
        preds = aligner.get_landmarks_from_image(rgb)
        if preds is None:
            preds = aligner.get_landmarks(rgb)
    except Exception:
        return None

    if not preds:
        return None

    def bbox_area(points: np.ndarray) -> float:
        x = points[:, 0]
        y = points[:, 1]
        return float((np.max(x) - np.min(x)) * (np.max(y) - np.min(y)))

    try:
        best = max((np.array(p, dtype=np.float32) for p in preds), key=bbox_area)
    except Exception:
        return None

    geometry = geometry_from_landmarks(best)
    if geometry is None:
        return None

    confidence = clamp01(0.62 + (0.18 * geometry["symmetry_score"]) + (0.10 * geometry["pose_score"]))
    return {
        "score": round6(geometry["score"]),
        "confidence": round6(confidence),
        "provider": "face_alignment_geometry",
        "model_version": model_version,
        "signals": {
            "provider_face_alignment_geometry": 1.0,
            "symmetry_score": round6(geometry["symmetry_score"]),
            "jaw_ratio": round6(geometry["jaw_ratio"]),
            "jaw_ratio_score": round6(geometry["jaw_ratio_score"]),
            "eye_mouth_ratio": round6(geometry["eye_mouth_ratio"]),
            "eye_mouth_score": round6(geometry["eye_mouth_score"]),
            "upper_lower_ratio": round6(geometry["upper_lower_ratio"]),
            "upper_lower_score": round6(geometry["upper_lower_score"]),
            "pose_score": round6(geometry["pose_score"]),
        },
    }


def heuristic_score(image_path: Path, model_version: str) -> Dict[str, Any]:
    image = cv2.imread(str(image_path))
    if image is None or image.size == 0:
        return {
            "score": 0.5,
            "confidence": 0.0,
            "provider": "face_alignment_geometry",
            "model_version": model_version,
            "signals": {
                "provider_heuristic": 1.0,
                "read_error": 1.0,
            },
        }

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
            symmetry = clamp01(
                1.0 - (float(np.mean(np.abs(left.astype(np.float32) - right_flip.astype(np.float32)))) / 255.0)
            )

    score = clamp01((0.40 * sharpness) + (0.35 * exposure) + (0.25 * symmetry))
    return {
        "score": round6(score),
        "confidence": 0.30,
        "provider": "face_alignment_geometry",
        "model_version": model_version,
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
    parser.add_argument("--repo-root", default="")
    parser.add_argument("--model-version", default=os.getenv("FACE_ALIGNMENT_MODEL_VERSION", "face_alignment_v1"))
    args = parser.parse_args()

    image_path = Path(args.image).resolve()
    repo_root = Path(args.repo_root).resolve() if args.repo_root else (
        Path(__file__).resolve().parents[3] / "third_party" / "repos" / "face-alignment"
    )

    payload = run_face_alignment(
        image_path=image_path,
        repo_root=repo_root,
        model_version=args.model_version,
    )
    if payload is None:
        payload = heuristic_score(image_path=image_path, model_version=args.model_version)

    print(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
