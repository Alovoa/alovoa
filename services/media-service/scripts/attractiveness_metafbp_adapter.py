#!/usr/bin/env python3
"""
MetaFBP adapter.

Attempts to run checkpoint inference inspired by:
  https://github.com/MetaVisionLab/MetaFBP

Supports two checkpoint styles:
1) Meta checkpoint with keys like `net.*` + `meta.weight`
2) Backbone checkpoint with logits head (`fc.weight`)

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
from typing import Any, Dict, Optional

import cv2
import numpy as np


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def round6(value: float) -> float:
    return round(float(value), 6)


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
    score = clamp01((0.60 * sharpness) + (0.40 * exposure))
    return {
        "score": score,
        "confidence": 0.30,
        "signals": {
            "provider_heuristic": 1.0,
            "sharpness": round6(sharpness),
            "exposure": round6(exposure),
        },
    }


def preprocess_image(image_path: Path, imgsz: int) -> Optional[np.ndarray]:
    image = cv2.imread(str(image_path))
    if image is None or image.size == 0:
        return None

    rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    resized = cv2.resize(rgb, (imgsz, imgsz), interpolation=cv2.INTER_CUBIC)

    # Center crop 224 like MetaFBP test pipeline.
    crop = 224
    start = max(0, (imgsz - crop) // 2)
    end = start + crop
    if resized.shape[0] >= crop and resized.shape[1] >= crop:
        patch = resized[start:end, start:end]
    else:
        patch = cv2.resize(resized, (crop, crop), interpolation=cv2.INTER_CUBIC)

    arr = patch.astype(np.float32) / 255.0
    mean = np.array([0.485, 0.456, 0.406], dtype=np.float32)
    std = np.array([0.229, 0.224, 0.225], dtype=np.float32)
    arr = (arr - mean) / std
    arr = np.transpose(arr, (2, 0, 1))
    return arr


def load_backbone(backbone: str, repo_root: Path):
    sys.path.insert(0, str(repo_root))
    from model.resnet import backbones  # type: ignore
    return backbones[backbone]


def extract_state_dict(checkpoint: Any) -> Optional[Dict[str, Any]]:
    if isinstance(checkpoint, dict):
        if "state_dict" in checkpoint and isinstance(checkpoint["state_dict"], dict):
            return checkpoint["state_dict"]
        # Sometimes checkpoint itself is already state dict.
        if any(isinstance(v, (np.ndarray,)) for v in checkpoint.values()):
            return checkpoint
        if any(hasattr(v, "shape") for v in checkpoint.values()):
            return checkpoint
    return checkpoint if isinstance(checkpoint, dict) else None


def run_metafbp(image_path: Path, model_path: Path, repo_root: Path, backbone: str, imgsz: int) -> Optional[Dict[str, Any]]:
    try:
        import torch
        import torch.nn.functional as F
    except Exception:
        return None

    if not model_path.exists() or not repo_root.exists():
        return None

    arr = preprocess_image(image_path, imgsz)
    if arr is None:
        return None

    try:
        checkpoint = torch.load(str(model_path), map_location="cpu")
    except Exception:
        return None

    state_dict = extract_state_dict(checkpoint)
    if not state_dict:
        return None

    model_class = load_backbone(backbone, repo_root)
    device = "cuda" if torch.cuda.is_available() else "cpu"

    # Path A: MAML-style checkpoint containing net.* + meta.weight.
    if any(k.startswith("net.") for k in state_dict.keys()) and "meta.weight" in state_dict:
        model = model_class(False, num_classes=1).to(device)
        model.eval()

        net_state = {k[len("net."):]: v for k, v in state_dict.items() if k.startswith("net.")}
        load = model.load_state_dict(net_state, strict=False)

        with torch.no_grad():
            x = torch.from_numpy(arr).unsqueeze(0).to(device=device, dtype=torch.float32)
            feat = model(x, feature_only=True)
            w = state_dict["meta.weight"].to(device)
            b = state_dict.get("meta.bias", None)
            if b is not None:
                b = b.to(device)
            raw = F.linear(feat, w, b).reshape(-1)[0]
            # MetaFBP maps to [0, 4] via sigmoid mapper for 5-way.
            score_0_4 = torch.sigmoid(raw) * 4.0
            normalized = clamp01(float(score_0_4.item()) / 4.0)

        try:
            missing = len(load.missing_keys)
            unexpected = len(load.unexpected_keys)
        except Exception:
            missing = 0
            unexpected = 0

        confidence = clamp01(0.70 - (0.001 * missing) - (0.001 * unexpected))
        return {
            "score": normalized,
            "confidence": confidence,
            "signals": {
                "provider_metafbp_meta": 1.0,
                "missing_keys": float(missing),
                "unexpected_keys": float(unexpected),
            },
        }

    # Path B: supervised backbone checkpoint (classification/regression).
    fc_weight = state_dict.get("fc.weight")
    if fc_weight is not None and hasattr(fc_weight, "shape"):
        out_dim = int(fc_weight.shape[0])
    else:
        out_dim = 1

    model = model_class(False, num_classes=out_dim).to(device)
    model.eval()

    cleaned = {}
    for k, v in state_dict.items():
        if k.startswith("module."):
            cleaned[k[len("module."):]] = v
        else:
            cleaned[k] = v

    load = model.load_state_dict(cleaned, strict=False)

    with torch.no_grad():
        x = torch.from_numpy(arr).unsqueeze(0).to(device=device, dtype=torch.float32)
        out = model(x).reshape(-1)
        if out_dim > 1:
            probs = torch.softmax(out, dim=0)
            idx = torch.arange(0, out_dim, device=probs.device, dtype=probs.dtype)
            expected = torch.sum(probs * idx) / max(1.0, float(out_dim - 1))
            normalized = clamp01(float(expected.item()))
            confidence = clamp01(float(torch.max(probs).item()))
        else:
            normalized = clamp01(float(torch.sigmoid(out[0]).item()))
            confidence = 0.60

    try:
        missing = len(load.missing_keys)
        unexpected = len(load.unexpected_keys)
    except Exception:
        missing = 0
        unexpected = 0

    confidence = clamp01(confidence - (0.001 * missing) - (0.001 * unexpected))
    return {
        "score": normalized,
        "confidence": confidence,
        "signals": {
            "provider_metafbp_backbone": 1.0,
            "output_dim": float(out_dim),
            "missing_keys": float(missing),
            "unexpected_keys": float(unexpected),
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True)
    parser.add_argument("--model", default=os.getenv("METAFBP_MODEL_PATH", ""))
    parser.add_argument("--repo-root", default="")
    parser.add_argument("--backbone", default=os.getenv("METAFBP_BACKBONE", "resnet18"))
    parser.add_argument("--imgsz", type=int, default=int(os.getenv("METAFBP_IMGSZ", "256")))
    args = parser.parse_args()

    image_path = Path(args.image).resolve()
    repo_root = Path(args.repo_root).resolve() if args.repo_root else (
        Path(__file__).resolve().parents[3] / "third_party" / "repos" / "MetaFBP"
    )

    payload = None
    if args.model:
        payload = run_metafbp(
            image_path=image_path,
            model_path=Path(args.model).resolve(),
            repo_root=repo_root,
            backbone=args.backbone,
            imgsz=max(224, int(args.imgsz)),
        )
    if payload is None:
        payload = heuristic_score(image_path)

    print(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
