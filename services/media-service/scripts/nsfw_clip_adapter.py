#!/usr/bin/env python3
"""
CLIP-based NSFW adapter inspired by LAION-AI/CLIP-based-NSFW-Detector.

Output contract:
{
  "nsfw_score": 0.0..1.0,
  "confidence": 0.0..1.0,
  "categories": {"nsfw": float, "sfw": float},
  "signals": { ... }
}
"""

from __future__ import annotations

import argparse
import json
import os
import zipfile
from functools import lru_cache
from pathlib import Path
from typing import Optional, Tuple

import cv2
import numpy as np
from PIL import Image


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def round6(value: float) -> float:
    return round(float(value), 6)


def skin_proxy(image: np.ndarray) -> float:
    ycrcb = cv2.cvtColor(image, cv2.COLOR_BGR2YCrCb)
    mask = cv2.inRange(ycrcb, (0, 133, 77), (255, 173, 127))
    ratio = float(np.mean(mask > 0))
    return clamp01((ratio - 0.10) / 0.55)


def _extract_default_model_if_needed(repo_root: Path, clip_model: str) -> Optional[Path]:
    if clip_model == "ViT-L/14":
        model_dir = repo_root / "clip_autokeras_binary_nsfw"
        zip_path = repo_root / "clip_autokeras_binary_nsfw.zip"
    elif clip_model == "ViT-B/32":
        model_dir = repo_root / "clip_autokeras_nsfw_b32"
        zip_path = repo_root / "clip_autokeras_nsfw_b32.zip"
    else:
        return None

    if model_dir.exists():
        return model_dir

    if not zip_path.exists():
        return None

    try:
        with zipfile.ZipFile(zip_path, "r") as zf:
            zf.extractall(repo_root)
    except Exception:
        return None

    if model_dir.exists():
        return model_dir
    return None


@lru_cache(maxsize=4)
def _load_clip_and_head(clip_model: str, model_dir_str: str):
    import autokeras as ak  # type: ignore
    import clip  # type: ignore
    import torch  # type: ignore
    from tensorflow.keras.models import load_model  # type: ignore

    device = "cuda" if torch.cuda.is_available() else "cpu"
    clip_model_obj, preprocess = clip.load(clip_model, device=device)

    head = load_model(model_dir_str, custom_objects=ak.CUSTOM_OBJECTS)
    # warmup
    dim = 768 if clip_model == "ViT-L/14" else 512
    head.predict(np.random.rand(1, dim).astype("float32"), batch_size=1, verbose=0)

    return clip_model_obj, preprocess, device, head


def score_with_clip_head(image_path: Path) -> Optional[Tuple[float, float]]:
    clip_model = os.getenv("CLIP_NSFW_CLIP_MODEL", "ViT-L/14")
    model_dir_env = os.getenv("CLIP_NSFW_MODEL_DIR", "").strip()

    model_dir = Path(model_dir_env).resolve() if model_dir_env else None
    if model_dir is None or not model_dir.exists():
        repo_root_env = os.getenv("CLIP_NSFW_REPO_ROOT", "").strip()
        if repo_root_env:
            repo_root = Path(repo_root_env).resolve()
        else:
            repo_root = (
                Path(__file__).resolve().parents[2]
                / "third_party"
                / "repos"
                / "CLIP-based-NSFW-Detector"
            )
        if repo_root.exists():
            model_dir = _extract_default_model_if_needed(repo_root, clip_model)

    if model_dir is None or not model_dir.exists():
        return None

    try:
        clip_model_obj, preprocess, device, head = _load_clip_and_head(clip_model, str(model_dir))
        import torch  # type: ignore

        image = preprocess(Image.open(image_path).convert("RGB")).unsqueeze(0).to(device)
        with torch.no_grad():
            embedding = clip_model_obj.encode_image(image).detach().cpu().numpy().astype("float32")
        score = float(head.predict(embedding, batch_size=1, verbose=0).reshape(-1)[0])
        score = clamp01(score)
        confidence = clamp01(0.70 + 0.25 * abs(score - 0.5) * 2.0)
        return score, confidence
    except Exception:
        return None


def fallback(image_path: Path) -> dict:
    image = cv2.imread(str(image_path))
    if image is None:
        return {
            "nsfw_score": 0.02,
            "confidence": 0.0,
            "categories": {
                "nsfw": 0.02,
                "sfw": 0.98,
            },
            "signals": {
                "provider_fallback": 1.0,
                "read_error": 1.0,
            },
        }

    proxy = skin_proxy(image)
    score = clamp01(0.05 + 0.72 * proxy)
    return {
        "nsfw_score": round6(score),
        "confidence": 0.35,
        "categories": {
            "nsfw": round6(score),
            "sfw": round6(1.0 - score),
        },
        "signals": {
            "provider_fallback": 1.0,
            "skin_proxy": round6(proxy),
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True)
    args = parser.parse_args()

    image_path = Path(args.image).resolve()
    scored = score_with_clip_head(image_path)
    if scored is None:
        print(json.dumps(fallback(image_path)))
        return 0

    score, confidence = scored
    payload = {
        "nsfw_score": round6(score),
        "confidence": round6(confidence),
        "categories": {
            "nsfw": round6(score),
            "sfw": round6(1.0 - score),
        },
        "signals": {
            "provider_clip_nsfw": 1.0,
            "clip_model": 1.0,
        },
    }
    print(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
