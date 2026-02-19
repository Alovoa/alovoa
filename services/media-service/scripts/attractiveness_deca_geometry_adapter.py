#!/usr/bin/env python3
"""
DECA geometry adapter.

Attempts to run:
  https://github.com/yfeng95/DECA

The adapter extracts 3D landmark geometry signals from DECA output and maps
them to a bounded attractiveness prior signal.

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
import shutil
import subprocess
import tempfile
from pathlib import Path
from typing import Any, Dict, Optional

import cv2
import numpy as np


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def round6(value: float) -> float:
    return round(float(value), 6)


def geometry_from_kpt3d(kpt: np.ndarray) -> Optional[Dict[str, float]]:
    if kpt.ndim != 2 or kpt.shape[1] < 3 or kpt.shape[0] < 20:
        return None

    # Common 68-point indices if available.
    n = kpt.shape[0]
    idx_nose = 30 if n > 30 else n // 2
    idx_chin = 8 if n > 8 else n - 1
    idx_brow = 27 if n > 27 else max(0, n // 3)
    idx_jaw_l = 4 if n > 12 else 0
    idx_jaw_r = 12 if n > 12 else n - 1

    jaw_width = float(np.linalg.norm(kpt[idx_jaw_l, :2] - kpt[idx_jaw_r, :2]))
    face_height = float(np.linalg.norm(kpt[idx_brow, :2] - kpt[idx_chin, :2]))
    if face_height <= 1e-6:
        return None

    ratio = jaw_width / face_height
    ratio_score = clamp01(1.0 - abs(ratio - 0.90) / 0.65)

    z = kpt[:, 2]
    z_span = float(np.max(z) - np.min(z))
    depth_score = clamp01(z_span / 120.0)

    center_x = float(kpt[idx_nose, 0])
    # Bilateral symmetry pairs (best-effort for 68 landmarks).
    pairs = [(1, 15), (2, 14), (3, 13), (4, 12), (5, 11), (6, 10), (7, 9)]
    diffs = []
    for l, r in pairs:
        if l >= n or r >= n:
            continue
        dl = abs(float(kpt[l, 0]) - center_x)
        dr = abs(float(kpt[r, 0]) - center_x)
        denom = max(dl, dr, 1e-6)
        diffs.append(abs(dl - dr) / denom)
    symmetry = clamp01(1.0 - float(np.mean(diffs))) if diffs else 0.5

    score = clamp01((0.38 * ratio_score) + (0.34 * symmetry) + (0.28 * depth_score))
    return {
        "score": score,
        "jaw_height_ratio": ratio,
        "ratio_score": ratio_score,
        "symmetry_score": symmetry,
        "depth_score": depth_score,
        "z_span": z_span,
    }


def run_deca(image_path: Path, repo_root: Path, timeout_sec: int, python_bin: str) -> Optional[Dict[str, Any]]:
    demo = repo_root / "demos" / "demo_reconstruct.py"
    if not demo.exists():
        return None

    with tempfile.TemporaryDirectory(prefix="aura_deca_in_") as in_dir, tempfile.TemporaryDirectory(prefix="aura_deca_out_") as out_dir:
        in_path = Path(in_dir) / "face.jpg"
        shutil.copyfile(str(image_path), str(in_path))

        cmd = [
            python_bin,
            str(demo),
            "-i", str(in_path),
            "-s", str(out_dir),
            "--device", "cpu",
            "--saveKpt", "true",
            "--saveVis", "false",
            "--saveObj", "false",
            "--saveMat", "false",
            "--saveDepth", "false",
            "--saveImages", "false",
            "--iscrop", "true",
            "--sample_step", "1",
        ]

        env = os.environ.copy()
        existing = env.get("PYTHONPATH", "")
        env["PYTHONPATH"] = str(repo_root) + (os.pathsep + existing if existing else "")

        try:
            proc = subprocess.run(
                cmd,
                cwd=str(repo_root),
                capture_output=True,
                text=True,
                timeout=max(1, timeout_sec),
                check=False,
                env=env,
            )
        except Exception:
            return None

        if proc.returncode != 0:
            return None

        kpt_files = list(Path(out_dir).rglob("*_kpt3d.txt"))
        if not kpt_files:
            return None

        try:
            kpt = np.loadtxt(str(kpt_files[0]), dtype=np.float32)
        except Exception:
            return None

        geometry = geometry_from_kpt3d(kpt)
        if geometry is None:
            return None

        return {
            "score": geometry["score"],
            "confidence": 0.70,
            "signals": {
                "provider_deca_geometry": 1.0,
                "jaw_height_ratio": round6(geometry["jaw_height_ratio"]),
                "ratio_score": round6(geometry["ratio_score"]),
                "symmetry_score": round6(geometry["symmetry_score"]),
                "depth_score": round6(geometry["depth_score"]),
                "z_span": round6(geometry["z_span"]),
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
    score = clamp01((0.5 * sharpness) + (0.5 * exposure))
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
    parser.add_argument("--repo-root", default="")
    parser.add_argument("--timeout-sec", type=int, default=int(os.getenv("DECA_TIMEOUT_SEC", "20")))
    parser.add_argument("--python-bin", default=os.getenv("DECA_PYTHON_BIN", "python3"))
    args = parser.parse_args()

    image_path = Path(args.image).resolve()
    repo_root = Path(args.repo_root).resolve() if args.repo_root else (
        Path(__file__).resolve().parents[3] / "third_party" / "repos" / "DECA"
    )

    payload = run_deca(
        image_path=image_path,
        repo_root=repo_root,
        timeout_sec=max(1, int(args.timeout_sec)),
        python_bin=args.python_bin,
    )
    if payload is None:
        payload = heuristic_score(image_path)

    print(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
