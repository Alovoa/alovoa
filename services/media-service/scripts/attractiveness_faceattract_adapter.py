#!/usr/bin/env python3
"""
FaceAttract adapter.

Attempts to run image-branch inference inspired by:
  https://github.com/fei-aiart/FaceAttract

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
import sys
from pathlib import Path
from typing import Any, Dict, Optional, Tuple

import cv2
import numpy as np


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def round6(value: float) -> float:
    return round(float(value), 6)


def normalize_model_score(raw: float) -> float:
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
    return max(0, int(x)), max(0, int(y)), max(1, int(w)), max(1, int(h))


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

    side = max(32, min(h, w))
    cx = w // 2
    cy = h // 2
    x1 = max(0, cx - side // 2)
    y1 = max(0, cy - side // 2)
    x2 = min(w, x1 + side)
    y2 = min(h, y1 + side)
    crop = image[y1:y2, x1:x2]
    return crop if crop.size > 0 else image


def run_faceattract(image_path: Path, model_path: Path, repo_root: Path) -> Optional[Dict[str, Any]]:
    try:
        import torch
        import torch.nn as nn
        import torch.nn.functional as F
    except Exception:
        return None

    scut_root = repo_root / "code" / "scut"
    if not scut_root.exists() or not model_path.exists():
        return None

    try:
        sys.path.insert(0, str(scut_root))
        from Mv2attn import MV2attn  # type: ignore
    except Exception:
        return None

    image = cv2.imread(str(image_path))
    if image is None or image.size == 0:
        return None

    cascade = (
        Path(__file__).resolve().parents[3] / "third_party" / "repos" / "BeautyPredict" / "common" / "haarcascade_frontalface_alt.xml"
    )
    face = detect_largest_face(image, cascade if cascade.exists() else None)
    crop = crop_face_or_center(image, face)
    resized = cv2.resize(crop, (224, 224), interpolation=cv2.INTER_CUBIC)
    rgb = cv2.cvtColor(resized, cv2.COLOR_BGR2RGB).astype(np.float32) / 255.0
    normed = (rgb - 0.5) / 0.5
    tensor = np.transpose(normed, (2, 0, 1))

    device = "cuda" if torch.cuda.is_available() else "cpu"
    model = MV2attn()
    # FaceAttract net checkpoints often replace L3 to output 1280-d vectors.
    model.attention.L3 = nn.Linear(in_features=1280, out_features=1280)
    model = model.to(device)
    model.eval()

    try:
        state = torch.load(str(model_path), map_location=device)
    except Exception:
        return None

    if isinstance(state, dict) and "state_dict" in state:
        state = state["state_dict"]

    if not isinstance(state, dict):
        return None

    filtered = {}
    for key, value in state.items():
        if key.startswith("image_net1.module."):
            filtered[key[len("image_net1.module."):]] = value
        elif key.startswith("image_net1."):
            filtered[key[len("image_net1."):]] = value
        elif key.startswith("module."):
            filtered[key[len("module."):]] = value
        elif key.startswith("features.") or key.startswith("attention."):
            filtered[key] = value

    if not filtered:
        return None

    try:
        load = model.load_state_dict(filtered, strict=False)
    except Exception:
        # Fallback to vanilla head if checkpoint contains 1x1280 attention output.
        model = MV2attn().to(device)
        model.eval()
        load = model.load_state_dict(filtered, strict=False)
    try:
        missing = len(load.missing_keys)
        unexpected = len(load.unexpected_keys)
    except Exception:
        missing = 0
        unexpected = 0

    with torch.no_grad():
        input_tensor = torch.from_numpy(tensor).unsqueeze(0).to(device=device, dtype=torch.float32)
        output = model(input_tensor)

        # If image branch returns 1280-d vector, project with top-level fc/fc1 from checkpoint.
        if output.ndim == 2 and output.shape[1] == 1280 and "fc.weight" in state and "fc1.weight" in state:
            x1 = torch.zeros_like(output)
            merged = torch.cat((x1, output), dim=1)  # [1, 2560]
            hidden = F.linear(
                merged,
                state["fc.weight"].to(device),
                state.get("fc.bias", None).to(device) if state.get("fc.bias", None) is not None else None,
            )
            projected = F.linear(
                hidden,
                state["fc1.weight"].to(device),
                state.get("fc1.bias", None).to(device) if state.get("fc1.bias", None) is not None else None,
            )
            raw = float(projected.detach().cpu().reshape(-1)[0])
        else:
            raw = float(output.detach().cpu().reshape(-1)[0])

    score = normalize_model_score(raw)
    confidence = clamp01(0.72 - (0.001 * missing) - (0.001 * unexpected))

    return {
        "score": score,
        "confidence": confidence,
        "signals": {
            "provider_faceattract": 1.0,
            "raw_model_output": round6(raw),
            "missing_keys": float(missing),
            "unexpected_keys": float(unexpected),
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
    symmetry = 0.5
    h, w = gray.shape[:2]
    if h > 8 and w > 8:
        mid = w // 2
        left = gray[:, :mid]
        right = gray[:, w - mid:]
        symmetry = clamp01(1.0 - (np.mean(np.abs(left.astype(np.float32) - cv2.flip(right, 1).astype(np.float32))) / 255.0))

    score = clamp01((0.45 * sharpness) + (0.30 * exposure) + (0.25 * symmetry))
    return {
        "score": score,
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
    parser.add_argument("--model", default=os.getenv("FACEATTRACT_MODEL_PATH", ""))
    parser.add_argument("--repo-root", default="")
    args = parser.parse_args()

    image_path = Path(args.image).resolve()
    repo_root = Path(args.repo_root).resolve() if args.repo_root else (
        Path(__file__).resolve().parents[3] / "third_party" / "repos" / "FaceAttract"
    )

    model_path = Path(args.model).resolve() if args.model else (
        repo_root / "code" / "pretrain_model" / "net_cross_1.weight"
    )

    payload = run_faceattract(image_path=image_path, model_path=model_path, repo_root=repo_root)
    if payload is None:
        payload = heuristic_score(image_path)

    print(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
