#!/usr/bin/env python3
"""
Deep3DFaceRecon geometry adapter.

Attempts to run:
  https://github.com/sicxu/Deep3DFaceRecon_pytorch

The adapter runs Deep3D reconstruction (when model/deps are available), reads
predicted coefficients/landmarks from output .mat, and converts geometry into
a bounded attractiveness prior signal.

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
import re
import shutil
import subprocess
import tempfile
from pathlib import Path
from typing import Any, Dict, Optional, Tuple

import cv2
import numpy as np


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def round6(value: float) -> float:
    return round(float(value), 6)


def detect_face_bbox(image: np.ndarray) -> Tuple[int, int, int, int]:
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
    faces = cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(40, 40))
    if len(faces) > 0:
        x, y, w, h = max(faces, key=lambda f: int(f[2]) * int(f[3]))
        return int(x), int(y), int(w), int(h)

    # center fallback
    h_img, w_img = gray.shape[:2]
    side = int(min(w_img, h_img) * 0.62)
    x = max(0, (w_img - side) // 2)
    y = max(0, (h_img - side) // 2)
    return x, y, side, side


def estimate_five_landmarks(image: np.ndarray) -> np.ndarray:
    x, y, w, h = detect_face_bbox(image)
    pts = np.array(
        [
            [x + 0.32 * w, y + 0.38 * h],  # left eye
            [x + 0.68 * w, y + 0.38 * h],  # right eye
            [x + 0.50 * w, y + 0.56 * h],  # nose
            [x + 0.38 * w, y + 0.75 * h],  # left mouth
            [x + 0.62 * w, y + 0.75 * h],  # right mouth
        ],
        dtype=np.float32,
    )
    return pts


def parse_epoch_from_path(path: Path) -> Optional[int]:
    m = re.search(r"epoch[_\-]?(\d+)\.pth$", path.name)
    if not m:
        return None
    try:
        return int(m.group(1))
    except Exception:
        return None


def autodetect_model(checkpoints_dir: Path, preferred_epoch: Optional[int]) -> Optional[Tuple[str, int]]:
    if not checkpoints_dir.exists():
        return None
    candidates = []
    for child in checkpoints_dir.iterdir():
        if not child.is_dir():
            continue
        epoch_files = list(child.glob("epoch_*.pth"))
        for ep in epoch_files:
            parsed = parse_epoch_from_path(ep)
            if parsed is not None:
                candidates.append((child.name, parsed, ep))
    if not candidates:
        return None

    if preferred_epoch is not None:
        exact = [c for c in candidates if c[1] == preferred_epoch]
        if exact:
            exact.sort(key=lambda x: x[0])
            return exact[0][0], exact[0][1]

    candidates.sort(key=lambda x: x[1], reverse=True)
    best = candidates[0]
    return best[0], best[1]


def symmetry_ratio_from_lm68(lm68: np.ndarray) -> Tuple[float, float]:
    if lm68.ndim == 3:
        lm68 = lm68[0]
    if lm68.ndim != 2 or lm68.shape[0] < 17 or lm68.shape[1] < 2:
        return 0.5, 0.5

    center_x = float(lm68[30, 0]) if lm68.shape[0] > 30 else float(np.mean(lm68[:, 0]))
    pairs = [(1, 15), (2, 14), (3, 13), (4, 12), (5, 11), (6, 10), (7, 9)]
    diffs = []
    for l, r in pairs:
        if l >= lm68.shape[0] or r >= lm68.shape[0]:
            continue
        dl = abs(float(lm68[l, 0]) - center_x)
        dr = abs(float(lm68[r, 0]) - center_x)
        denom = max(dl, dr, 1e-6)
        diffs.append(abs(dl - dr) / denom)

    symmetry = clamp01(1.0 - float(np.mean(diffs))) if diffs else 0.5

    jaw_l = lm68[0] if lm68.shape[0] > 0 else lm68[0]
    jaw_r = lm68[16] if lm68.shape[0] > 16 else lm68[-1]
    chin = lm68[8] if lm68.shape[0] > 8 else lm68[lm68.shape[0] // 2]
    brow = lm68[27] if lm68.shape[0] > 27 else lm68[max(0, lm68.shape[0] // 3)]

    jaw_width = float(np.linalg.norm(jaw_l[:2] - jaw_r[:2]))
    face_height = float(np.linalg.norm(chin[:2] - brow[:2]))
    if face_height <= 1e-6:
        ratio_score = 0.5
    else:
        ratio = jaw_width / face_height
        ratio_score = clamp01(1.0 - abs(ratio - 0.90) / 0.65)

    return symmetry, ratio_score


def parse_coeff_features(mat_payload: Dict[str, Any]) -> Dict[str, float]:
    exp_norm = 0.0
    pose_score = 0.5
    trans_stability = 0.5

    if "exp" in mat_payload:
        exp = np.array(mat_payload["exp"], dtype=np.float32).reshape(-1)
        if exp.size > 0:
            exp_norm = float(np.linalg.norm(exp) / np.sqrt(float(exp.size)))

    if "angle" in mat_payload:
        angle = np.array(mat_payload["angle"], dtype=np.float32).reshape(-1)
        if angle.size >= 3:
            pose_mag = float(np.linalg.norm(angle[:3]))
            pose_score = clamp01(1.0 - (pose_mag / 1.1))

    if "trans" in mat_payload:
        trans = np.array(mat_payload["trans"], dtype=np.float32).reshape(-1)
        if trans.size > 0:
            trans_mag = float(np.linalg.norm(trans))
            trans_stability = clamp01(1.0 - (trans_mag / 6.0))

    exp_score = clamp01(1.0 - (exp_norm / 2.5))
    return {
        "exp_norm": exp_norm,
        "exp_score": exp_score,
        "pose_score": pose_score,
        "trans_stability": trans_stability,
    }


def run_deep3d(
    image_path: Path,
    repo_root: Path,
    checkpoints_dir: Path,
    model_name: str,
    epoch: int,
    timeout_sec: int,
    python_bin: str,
) -> Optional[Dict[str, Any]]:
    try:
        from scipy.io import loadmat
    except Exception:
        return None

    test_py = repo_root / "test.py"
    if not test_py.exists():
        return None

    model_file = checkpoints_dir / model_name / f"epoch_{epoch}.pth"
    if not model_file.exists():
        return None

    with tempfile.TemporaryDirectory(prefix="aura_deep3d_") as tmp_root:
        tmp_root_path = Path(tmp_root)
        case_dir = tmp_root_path / "input"
        det_dir = case_dir / "detections"
        case_dir.mkdir(parents=True, exist_ok=True)
        det_dir.mkdir(parents=True, exist_ok=True)

        image = cv2.imread(str(image_path))
        if image is None or image.size == 0:
            return None

        img_name = "face.jpg"
        img_case_path = case_dir / img_name
        cv2.imwrite(str(img_case_path), image)

        lm5 = estimate_five_landmarks(image)
        np.savetxt(str(det_dir / "face.txt"), lm5, fmt="%.6f")

        cmd = [
            python_bin,
            str(test_py),
            "--name", model_name,
            "--epoch", str(epoch),
            "--img_folder", str(case_dir),
            "--gpu_ids", "-1",
            "--checkpoints_dir", str(checkpoints_dir),
            "--bfm_folder", str(repo_root / "BFM"),
            "--use_opengl", "False",
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

        out_mat = checkpoints_dir / model_name / "results" / case_dir.name / f"epoch_{epoch}_000000" / "face.mat"
        if not out_mat.exists():
            return None

        try:
            mat = loadmat(str(out_mat))
        except Exception:
            return None

        lm68 = mat.get("lm68", None)
        if lm68 is None:
            return None

        symmetry, ratio_score = symmetry_ratio_from_lm68(np.array(lm68, dtype=np.float32))
        coeff = parse_coeff_features(mat)

        score = clamp01(
            (0.32 * symmetry)
            + (0.28 * ratio_score)
            + (0.24 * coeff["pose_score"])
            + (0.16 * coeff["exp_score"])
        )
        confidence = clamp01(0.72 * coeff["trans_stability"] + 0.20)

        return {
            "score": score,
            "confidence": confidence,
            "signals": {
                "provider_deep3d_geometry": 1.0,
                "symmetry_score": round6(symmetry),
                "ratio_score": round6(ratio_score),
                "pose_score": round6(coeff["pose_score"]),
                "exp_score": round6(coeff["exp_score"]),
                "exp_norm": round6(coeff["exp_norm"]),
                "trans_stability": round6(coeff["trans_stability"]),
                "epoch_used": float(epoch),
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
    score = clamp01((0.52 * sharpness) + (0.48 * exposure))
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
    parser.add_argument("--model-name", default=os.getenv("DEEP3D_MODEL_NAME", ""))
    parser.add_argument("--epoch", type=int, default=int(os.getenv("DEEP3D_EPOCH", "20")))
    parser.add_argument("--checkpoints-dir", default=os.getenv("DEEP3D_CHECKPOINTS_DIR", ""))
    parser.add_argument("--python-bin", default=os.getenv("DEEP3D_PYTHON_BIN", "python3"))
    parser.add_argument("--timeout-sec", type=int, default=int(os.getenv("DEEP3D_TIMEOUT_SEC", "45")))
    args = parser.parse_args()

    image_path = Path(args.image).resolve()
    repo_root = Path(args.repo_root).resolve() if args.repo_root else (
        Path(__file__).resolve().parents[3] / "third_party" / "repos" / "Deep3DFaceRecon_pytorch"
    )
    checkpoints_dir = (
        Path(args.checkpoints_dir).resolve()
        if args.checkpoints_dir
        else (repo_root / "checkpoints").resolve()
    )

    model_name = args.model_name.strip()
    epoch = int(args.epoch)

    if not model_name:
        detected = autodetect_model(checkpoints_dir, preferred_epoch=epoch)
        if detected is not None:
            model_name, epoch = detected

    payload = None
    if model_name:
        payload = run_deep3d(
            image_path=image_path,
            repo_root=repo_root,
            checkpoints_dir=checkpoints_dir,
            model_name=model_name,
            epoch=epoch,
            timeout_sec=max(1, int(args.timeout_sec)),
            python_bin=args.python_bin,
        )
    if payload is None:
        payload = heuristic_score(image_path)

    print(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
