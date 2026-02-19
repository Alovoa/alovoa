#!/usr/bin/env python3
"""
dlib geometry adapter.

Attempts to run:
  https://github.com/davisking/dlib

Uses dlib frontal detector and optional 68-point shape predictor to derive
geometry signals and a bounded attractiveness prior.

Output contract:
{
  "score": 0.0..1.0,
  "confidence": 0.0..1.0,
  "provider": "dlib_geometry",
  "model_version": "...",
  "signals": { ... }
}
"""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
from typing import Any, Dict, Optional, Tuple

import cv2
import numpy as np

try:
    import dlib  # type: ignore
    DLIB_AVAILABLE = True
except Exception:
    dlib = None
    DLIB_AVAILABLE = False


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def round6(value: float) -> float:
    return round(float(value), 6)


def target_closeness(value: float, target: float, tolerance: float) -> float:
    return clamp01(1.0 - abs(value - target) / max(tolerance, 1e-6))


def dlib_face_and_landmarks(
    image_bgr: np.ndarray,
    predictor_path: Optional[Path],
) -> Tuple[Optional[Tuple[int, int, int, int]], Optional[np.ndarray]]:
    if not DLIB_AVAILABLE:
        return None, None

    try:
        detector = dlib.get_frontal_face_detector()
    except Exception:
        return None, None

    gray = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2GRAY)
    rects = detector(gray, 1)
    if not rects:
        return None, None

    best = max(rects, key=lambda r: max(0, r.right() - r.left()) * max(0, r.bottom() - r.top()))
    x1, y1 = max(0, best.left()), max(0, best.top())
    x2, y2 = max(x1 + 1, best.right()), max(y1 + 1, best.bottom())
    bbox = (x1, y1, x2 - x1, y2 - y1)

    if predictor_path is None or not predictor_path.exists():
        return bbox, None

    try:
        predictor = dlib.shape_predictor(str(predictor_path))
        shape = predictor(gray, best)
        points = np.array([[shape.part(i).x, shape.part(i).y] for i in range(shape.num_parts)], dtype=np.float32)
        if points.shape[0] < 68:
            return bbox, None
        return bbox, points
    except Exception:
        return bbox, None


def geometry_from_landmarks(points: np.ndarray) -> Optional[Dict[str, float]]:
    if points.ndim != 2 or points.shape[0] < 68 or points.shape[1] < 2:
        return None

    def dist(i: int, j: int) -> float:
        return float(np.linalg.norm(points[i, :2] - points[j, :2]))

    center_x = float(points[30, 0])
    pairs = [(1, 15), (2, 14), (3, 13), (4, 12), (5, 11), (6, 10), (7, 9)]
    asym = []
    for li, ri in pairs:
        dl = abs(float(points[li, 0]) - center_x)
        dr = abs(float(points[ri, 0]) - center_x)
        asym.append(abs(dl - dr) / max(dl, dr, 1e-6))
    symmetry = clamp01(1.0 - float(np.mean(asym))) if asym else 0.5

    jaw_width = dist(0, 16)
    face_height = dist(27, 8)
    interocular = dist(36, 45)
    mouth_width = dist(48, 54)
    nose_height = dist(27, 33)
    lower_height = dist(33, 8)

    if min(jaw_width, face_height, interocular, mouth_width, nose_height, lower_height) <= 1e-6:
        return None

    jaw_ratio = jaw_width / face_height
    eye_mouth_ratio = interocular / mouth_width
    upper_lower_ratio = nose_height / lower_height
    eye_line_angle = abs(float(np.arctan2(points[45, 1] - points[36, 1], points[45, 0] - points[36, 0])))

    jaw_score = target_closeness(jaw_ratio, 0.90, 0.60)
    eye_mouth_score = target_closeness(eye_mouth_ratio, 0.75, 0.50)
    upper_lower_score = target_closeness(upper_lower_ratio, 0.95, 0.55)
    pose_score = clamp01(1.0 - eye_line_angle / 0.70)

    score = clamp01(
        (0.35 * symmetry)
        + (0.20 * jaw_score)
        + (0.18 * eye_mouth_score)
        + (0.17 * upper_lower_score)
        + (0.10 * pose_score)
    )

    return {
        "score": score,
        "symmetry_score": symmetry,
        "jaw_ratio": jaw_ratio,
        "jaw_ratio_score": jaw_score,
        "eye_mouth_ratio": eye_mouth_ratio,
        "eye_mouth_score": eye_mouth_score,
        "upper_lower_ratio": upper_lower_ratio,
        "upper_lower_score": upper_lower_score,
        "pose_score": pose_score,
    }


def bbox_geometry_score(image: np.ndarray, bbox: Tuple[int, int, int, int]) -> Dict[str, float]:
    h, w = image.shape[:2]
    x, y, bw, bh = bbox
    area_ratio = clamp01((bw * bh) / max(1.0, float(w * h)))
    ratio = bw / max(1.0, bh)
    area_score = target_closeness(area_ratio, 0.30, 0.25)
    ratio_score = target_closeness(ratio, 0.78, 0.40)
    score = clamp01(0.55 * area_score + 0.45 * ratio_score)
    return {
        "score": score,
        "bbox_area_ratio": area_ratio,
        "bbox_aspect_ratio": ratio,
        "bbox_area_score": area_score,
        "bbox_ratio_score": ratio_score,
    }


def heuristic_score(image: np.ndarray) -> Dict[str, float]:
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

    score = clamp01(0.40 * sharpness + 0.35 * exposure + 0.25 * symmetry)
    return {
        "score": score,
        "sharpness": sharpness,
        "exposure": exposure,
        "symmetry": symmetry,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True)
    parser.add_argument("--predictor", default=os.getenv("DLIB_PREDICTOR_PATH", ""))
    parser.add_argument("--model-version", default=os.getenv("DLIB_MODEL_VERSION", "dlib_geometry_v1"))
    args = parser.parse_args()

    image_path = Path(args.image).resolve()
    image = cv2.imread(str(image_path))
    if image is None or image.size == 0:
        payload = {
            "score": 0.5,
            "confidence": 0.0,
            "provider": "dlib_geometry",
            "model_version": args.model_version,
            "signals": {
                "provider_heuristic": 1.0,
                "read_error": 1.0,
            },
        }
        print(json.dumps(payload))
        return 0

    predictor_path = Path(args.predictor).resolve() if args.predictor else None
    bbox, landmarks = dlib_face_and_landmarks(image, predictor_path)

    if landmarks is not None:
        g = geometry_from_landmarks(landmarks)
        if g is not None:
            confidence = clamp01(0.62 + 0.20 * g["symmetry_score"] + 0.10 * g["pose_score"])
            payload = {
                "score": round6(g["score"]),
                "confidence": round6(confidence),
                "provider": "dlib_geometry",
                "model_version": args.model_version,
                "signals": {
                    "provider_dlib_geometry": 1.0,
                    "dlib_available": 1.0 if DLIB_AVAILABLE else 0.0,
                    "predictor_loaded": 1.0,
                    "symmetry_score": round6(g["symmetry_score"]),
                    "jaw_ratio": round6(g["jaw_ratio"]),
                    "jaw_ratio_score": round6(g["jaw_ratio_score"]),
                    "eye_mouth_ratio": round6(g["eye_mouth_ratio"]),
                    "eye_mouth_score": round6(g["eye_mouth_score"]),
                    "upper_lower_ratio": round6(g["upper_lower_ratio"]),
                    "upper_lower_score": round6(g["upper_lower_score"]),
                    "pose_score": round6(g["pose_score"]),
                },
            }
            print(json.dumps(payload))
            return 0

    if bbox is not None:
        g = bbox_geometry_score(image, bbox)
        payload = {
            "score": round6(g["score"]),
            "confidence": 0.45,
            "provider": "dlib_geometry",
            "model_version": args.model_version,
            "signals": {
                "provider_dlib_bbox": 1.0,
                "dlib_available": 1.0 if DLIB_AVAILABLE else 0.0,
                "predictor_loaded": 0.0,
                "bbox_area_ratio": round6(g["bbox_area_ratio"]),
                "bbox_aspect_ratio": round6(g["bbox_aspect_ratio"]),
                "bbox_area_score": round6(g["bbox_area_score"]),
                "bbox_ratio_score": round6(g["bbox_ratio_score"]),
            },
        }
        print(json.dumps(payload))
        return 0

    h = heuristic_score(image)
    payload = {
        "score": round6(h["score"]),
        "confidence": 0.30,
        "provider": "dlib_geometry",
        "model_version": args.model_version,
        "signals": {
            "provider_heuristic": 1.0,
            "dlib_available": 1.0 if DLIB_AVAILABLE else 0.0,
            "predictor_loaded": 0.0,
            "sharpness": round6(h["sharpness"]),
            "exposure": round6(h["exposure"]),
            "symmetry": round6(h["symmetry"]),
        },
    }
    print(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
