#!/usr/bin/env python3
"""
Silent-Face anti-spoof adapter for media-service ANTISPOOF_EXTERNAL_CMD.

Output contract (stdout JSON):
{
  "liveness": 0.0..1.0,
  "spoof_prob": 0.0..1.0,
  "confidence": 0.0..1.0,
  "signals": { ... }
}
"""

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import uuid
from pathlib import Path

import cv2
import numpy as np


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def parse_silent_face_stdout(stdout: str) -> dict | None:
    text = stdout or ""
    score_match = re.search(r"Score:\s*([0-9]+(?:\.[0-9]+)?)", text)
    raw_score = float(score_match.group(1)) if score_match else 0.5
    raw_score = clamp01(raw_score)

    if "Real Face" in text:
        liveness = raw_score
        spoof_prob = 1.0 - liveness
    elif "Fake Face" in text:
        spoof_prob = raw_score
        liveness = 1.0 - spoof_prob
    else:
        return None

    return {
        "liveness": clamp01(liveness),
        "spoof_prob": clamp01(spoof_prob),
        "confidence": 0.75,
        "signals": {
            "engine": 1.0,
            "model_score": raw_score,
        },
    }


def run_silent_face(repo_root: Path, image_path: Path, timeout_sec: int) -> dict | None:
    test_py = repo_root / "test.py"
    sample_dir = repo_root / "images" / "sample"
    model_dir = repo_root / "resources" / "anti_spoof_models"

    if not test_py.exists() or not sample_dir.exists() or not model_dir.exists():
        return None

    sample_name = f"aura_{uuid.uuid4().hex}.jpg"
    sample_path = sample_dir / sample_name
    try:
        shutil.copyfile(str(image_path), str(sample_path))

        cmd = [
            sys.executable,
            "test.py",
            "--image_name",
            sample_name,
            "--model_dir",
            str(model_dir),
            "--device_id",
            os.getenv("SILENT_FACE_DEVICE_ID", "0"),
        ]

        env = os.environ.copy()
        existing_pythonpath = env.get("PYTHONPATH", "")
        env["PYTHONPATH"] = str(repo_root) + (os.pathsep + existing_pythonpath if existing_pythonpath else "")

        proc = subprocess.run(
            cmd,
            cwd=str(repo_root),
            capture_output=True,
            text=True,
            timeout=max(1, timeout_sec),
            check=False,
            env=env,
        )
        if proc.returncode != 0:
            return None
        parsed = parse_silent_face_stdout(proc.stdout)
        if parsed is None:
            return None
        parsed["signals"]["provider_silent_face"] = 1.0
        return parsed
    except Exception:
        return None
    finally:
        if sample_path.exists():
            try:
                sample_path.unlink()
            except Exception:
                pass


def heuristic_antispoof(image_path: Path) -> dict:
    image = cv2.imread(str(image_path))
    if image is None or image.size == 0:
        return {
            "liveness": 0.5,
            "spoof_prob": 0.5,
            "confidence": 0.0,
            "signals": {"read_error": 1.0},
        }

    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    sharpness = clamp01(float(cv2.Laplacian(gray, cv2.CV_64F).var()) / 500.0)
    contrast = clamp01(float(np.std(gray)) / 64.0)
    illumination = clamp01(1.0 - abs(float(np.mean(gray)) - 127.5) / 127.5)

    spoof_prob = clamp01((0.55 * (1.0 - sharpness)) + (0.30 * (1.0 - contrast)) + (0.15 * (1.0 - illumination)))
    liveness = clamp01(1.0 - spoof_prob)

    return {
        "liveness": liveness,
        "spoof_prob": spoof_prob,
        "confidence": 0.35,
        "signals": {
            "provider_heuristic": 1.0,
            "sharpness": sharpness,
            "contrast": contrast,
            "illumination": illumination,
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True)
    parser.add_argument("--repo-root", default="")
    parser.add_argument("--timeout-sec", type=int, default=8)
    args = parser.parse_args()

    image_path = Path(args.image).resolve()
    repo_root = Path(args.repo_root).resolve() if args.repo_root else (
        Path(__file__).resolve().parents[3] / "third_party" / "repos" / "Silent-Face-Anti-Spoofing"
    )

    payload = run_silent_face(repo_root, image_path, args.timeout_sec)
    if payload is None:
        payload = heuristic_antispoof(image_path)

    sys.stdout.write(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

