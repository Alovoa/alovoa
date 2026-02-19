#!/usr/bin/env python3
"""
BeautyPredict adapter.

Attempts to run inference inspired by:
  https://github.com/ustcqidi/BeautyPredict

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
from pathlib import Path
from typing import Any, Dict, Optional, Tuple

import cv2
import numpy as np


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def round6(value: float) -> float:
    return round(float(value), 6)


def normalize_raw_beauty(raw: float) -> float:
    # Common attractiveness outputs are around [1,5].
    if 1.0 <= raw <= 5.0:
        return clamp01((raw - 1.0) / 4.0)
    if 0.0 <= raw <= 1.0:
        return clamp01(raw)
    if 0.0 <= raw <= 10.0:
        return clamp01(raw / 10.0)
    return clamp01(1.0 / (1.0 + np.exp(-raw / 2.0)))


def detect_largest_face(image: np.ndarray, cascade_path: Optional[Path]) -> Optional[Tuple[int, int, int, int]]:
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    faces = []

    if cascade_path is not None and cascade_path.exists():
        try:
            detector = cv2.CascadeClassifier(str(cascade_path))
            if not detector.empty():
                faces = detector.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(40, 40))
        except Exception:
            faces = []

    if len(faces) == 0:
        return None

    x, y, w, h = max(faces, key=lambda item: int(item[2]) * int(item[3]))
    x = max(0, int(x))
    y = max(0, int(y))
    w = max(1, int(w))
    h = max(1, int(h))
    return x, y, w, h


def crop_face_or_center(image: np.ndarray, face: Optional[Tuple[int, int, int, int]]) -> np.ndarray:
    h, w = image.shape[:2]
    if face is not None:
        x, y, fw, fh = face
        pad_w = int(0.15 * fw)
        pad_h = int(0.15 * fh)
        x1 = max(0, x - pad_w)
        y1 = max(0, y - pad_h)
        x2 = min(w, x + fw + pad_w)
        y2 = min(h, y + fh + pad_h)
        crop = image[y1:y2, x1:x2]
        if crop.size > 0:
            return crop

    # fallback center crop
    side = max(32, min(h, w))
    cx = w // 2
    cy = h // 2
    x1 = max(0, cx - side // 2)
    y1 = max(0, cy - side // 2)
    x2 = min(w, x1 + side)
    y2 = min(h, y1 + side)
    crop = image[y1:y2, x1:x2]
    return crop if crop.size > 0 else image


def run_beautypredict(image_path: Path, model_path: Path, repo_root: Path) -> Optional[Dict[str, Any]]:
    try:
        from tensorflow import keras  # type: ignore
    except Exception:
        return None

    if not model_path.exists():
        return None

    image = cv2.imread(str(image_path))
    if image is None or image.size == 0:
        return None

    cascade = repo_root / "common" / "haarcascade_frontalface_alt.xml"
    face = detect_largest_face(image, cascade if cascade.exists() else None)
    crop = crop_face_or_center(image, face)
    resized = cv2.resize(crop, (224, 224), interpolation=cv2.INTER_CUBIC)
    # Match BeautyPredict preprocessing: (img - 127.5) / 127.5
    normed = (resized.astype(np.float32) - 127.5) / 127.5
    batch = np.expand_dims(normed, axis=0)

    try:
        model = keras.models.load_model(str(model_path), compile=False)
        pred = model.predict(batch, verbose=0)
    except Exception:
        return None

    pred = np.array(pred).reshape(-1)
    if pred.size == 0:
        return None

    if pred.size >= 5:
        probs = pred[:5].astype(np.float32)
        total = float(np.sum(probs))
        if total <= 0.0:
            return None
        probs = probs / total
        expected = float(np.sum(np.arange(1, 6, dtype=np.float32) * probs))
        score = normalize_raw_beauty(expected)
        confidence = clamp01(float(np.max(probs)))
        return {
            "score": score,
            "confidence": confidence,
            "signals": {
                "provider_beautypredict": 1.0,
                "raw_expected_score": round6(expected),
                "face_detected": 1.0 if face is not None else 0.0,
            },
        }

    raw = float(pred[0])
    score = normalize_raw_beauty(raw)
    return {
        "score": score,
        "confidence": 0.60,
        "signals": {
            "provider_beautypredict": 1.0,
            "raw_model_output": round6(raw),
            "face_detected": 1.0 if face is not None else 0.0,
        },
    }


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


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True)
    parser.add_argument("--model", default=os.getenv("BEAUTYPREDICT_MODEL_PATH", ""))
    parser.add_argument("--repo-root", default="")
    args = parser.parse_args()

    image_path = Path(args.image).resolve()
    repo_root = Path(args.repo_root).resolve() if args.repo_root else (
        Path(__file__).resolve().parents[3] / "third_party" / "repos" / "BeautyPredict"
    )

    payload = None
    if args.model:
        payload = run_beautypredict(
            image_path=image_path,
            model_path=Path(args.model).resolve(),
            repo_root=repo_root,
        )
    if payload is None:
        payload = heuristic_score(image_path)

    print(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
