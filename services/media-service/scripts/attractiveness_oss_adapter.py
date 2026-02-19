#!/usr/bin/env python3
"""
OSS attractiveness adapter for TS orchestrator local mode.

Primary stack:
- insightface (face detection + pose confidence)
- mediapipe (optional landmark symmetry refinement)
- cv2/numpy fallback heuristics (always available path)

Output contract:
{
  "score": 0.0..1.0,
  "confidence": 0.0..1.0,
  "signals": { ... numeric ... }
}
"""

import argparse
import json
import os
import sys
from typing import Optional, Dict, Any

import cv2
import numpy as np

try:
    from insightface.app import FaceAnalysis
    INSIGHTFACE_AVAILABLE = True
except Exception:
    FaceAnalysis = None
    INSIGHTFACE_AVAILABLE = False

try:
    import mediapipe as mp
    MEDIAPIPE_AVAILABLE = True
except Exception:
    mp = None
    MEDIAPIPE_AVAILABLE = False

_INSIGHT_APP = None
_INSIGHT_FAILED = False


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def round6(value: float) -> float:
    return round(float(value), 6)


def normalize_sharpness(gray: np.ndarray) -> float:
    variance = float(cv2.Laplacian(gray, cv2.CV_64F).var())
    return clamp01(variance / 500.0)


def normalize_exposure(gray: np.ndarray) -> float:
    mean = float(np.mean(gray))
    return clamp01(1.0 - (abs(mean - 127.5) / 127.5))


def estimate_symmetry(gray: np.ndarray) -> float:
    h, w = gray.shape[:2]
    if h < 8 or w < 8:
        return 0.5
    mid = w // 2
    if mid < 4 or (w - mid) < 4:
        return 0.5
    left = gray[:, :mid]
    right = gray[:, w - mid:]
    right_flip = cv2.flip(right, 1)
    diff = np.mean(np.abs(left.astype(np.float32) - right_flip.astype(np.float32))) / 255.0
    return clamp01(1.0 - diff)


def mediapipe_symmetry(image: np.ndarray) -> Optional[float]:
    if not MEDIAPIPE_AVAILABLE:
        return None
    try:
        rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        mesh = mp.solutions.face_mesh.FaceMesh(
            static_image_mode=True,
            max_num_faces=1,
            refine_landmarks=False,
            min_detection_confidence=0.5,
        )
        result = mesh.process(rgb)
        mesh.close()
        if not result.multi_face_landmarks:
            return None
        landmarks = result.multi_face_landmarks[0].landmark
        center = landmarks[1]
        pairs = [
            (33, 263), (61, 291), (159, 386), (145, 374),
            (93, 323), (132, 361), (58, 288), (172, 397),
        ]
        deltas = []
        for left_idx, right_idx in pairs:
            left = landmarks[left_idx]
            right = landmarks[right_idx]
            dl = np.hypot(left.x - center.x, left.y - center.y)
            dr = np.hypot(right.x - center.x, right.y - center.y)
            denom = max(dl, dr, 1e-6)
            deltas.append(abs(dl - dr) / denom)
        if not deltas:
            return None
        return clamp01(1.0 - float(np.mean(deltas)))
    except Exception:
        return None


def get_insight_app():
    global _INSIGHT_APP, _INSIGHT_FAILED
    if _INSIGHT_FAILED or not INSIGHTFACE_AVAILABLE:
        return None
    if _INSIGHT_APP is not None:
        return _INSIGHT_APP
    try:
        model_name = os.getenv("INSIGHTFACE_MODEL_NAME", "buffalo_l")
        provider_env = os.getenv("INSIGHTFACE_PROVIDERS", "CPUExecutionProvider")
        providers = [p.strip() for p in provider_env.split(",") if p.strip()]
        ctx_id = int(os.getenv("INSIGHTFACE_CTX_ID", "-1"))
        det_size = int(os.getenv("INSIGHTFACE_DET_SIZE", "640"))
        app = FaceAnalysis(name=model_name, providers=providers)
        app.prepare(ctx_id=ctx_id, det_size=(det_size, det_size))
        _INSIGHT_APP = app
    except Exception:
        _INSIGHT_FAILED = True
        _INSIGHT_APP = None
    return _INSIGHT_APP


def insightface_signals(image: np.ndarray) -> Optional[Dict[str, float]]:
    app = get_insight_app()
    if app is None:
        return None
    try:
        faces = app.get(image)
        if not faces:
            return None
        face = max(faces, key=lambda f: float(getattr(f, "det_score", 0.0)))
        h, w = image.shape[:2]
        bbox = getattr(face, "bbox", None)
        if bbox is None or len(bbox) < 4:
            return None
        x1, y1, x2, y2 = [float(v) for v in bbox[:4]]
        face_w = max(0.0, x2 - x1)
        face_h = max(0.0, y2 - y1)
        area_ratio = clamp01((face_w * face_h) / max(1.0, float(w * h)))

        pose = getattr(face, "pose", None)
        yaw_norm = 0.5
        pitch_norm = 0.5
        if pose is not None and len(pose) >= 2:
            # Typical useful yaw/pitch range ~[-45,45] for selfies.
            yaw_norm = clamp01(abs(float(pose[1])) / 45.0)
            pitch_norm = clamp01(abs(float(pose[0])) / 45.0)

        return {
            "insight_det": clamp01(float(getattr(face, "det_score", 0.0))),
            "insight_fill": area_ratio,
            "insight_yaw": yaw_norm,
            "insight_pitch": pitch_norm,
            "insight_face_count": clamp01(len(faces) / 3.0),
        }
    except Exception:
        return None


def framing_score_from_bbox(signals: Optional[Dict[str, float]]) -> float:
    if not signals:
        return 0.5
    fill = clamp01(float(signals.get("insight_fill", 0.5)))
    # Best framing often around ~0.30 face area for selfies.
    return clamp01(1.0 - abs(fill - 0.30) / 0.30)


def compute_score(image: np.ndarray, view: str) -> Dict[str, Any]:
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    sharpness = normalize_sharpness(gray)
    exposure = normalize_exposure(gray)
    symmetry_basic = estimate_symmetry(gray)
    symmetry_mesh = mediapipe_symmetry(image)
    symmetry = symmetry_mesh if symmetry_mesh is not None else symmetry_basic

    insight = insightface_signals(image)
    framing = framing_score_from_bbox(insight)

    # Base quality/geometry priors.
    base_score = (
        0.32 * sharpness
        + 0.24 * exposure
        + 0.24 * symmetry
        + 0.20 * framing
    )

    insight_det = 0.0
    front_pose_bonus = 0.0
    side_profile_bonus = 0.0
    insight_conf = 0.0
    if insight is not None:
        insight_det = clamp01(float(insight.get("insight_det", 0.0)))
        yaw = clamp01(float(insight.get("insight_yaw", 0.5)))
        pitch = clamp01(float(insight.get("insight_pitch", 0.5)))

        # Front images prefer lower yaw/pitch, side images prefer moderate yaw.
        front_pose_bonus = clamp01(1.0 - (0.7 * yaw + 0.3 * pitch))
        target_side_yaw = 0.75  # ~34 degrees out of 45.
        side_profile_bonus = clamp01(1.0 - abs(yaw - target_side_yaw) / target_side_yaw)

        if view == "side":
            score = (0.74 * base_score) + (0.16 * insight_det) + (0.10 * side_profile_bonus)
        else:
            score = (0.74 * base_score) + (0.16 * insight_det) + (0.10 * front_pose_bonus)
        insight_conf = 0.55 + 0.45 * insight_det
    else:
        score = base_score

    score = clamp01(score)
    confidence = clamp01((0.45 * sharpness) + (0.35 * exposure) + (0.20 * max(insight_conf, 0.4)))

    signals: Dict[str, float] = {
        "sharpness": round6(sharpness),
        "exposure": round6(exposure),
        "symmetry": round6(symmetry),
        "symmetry_mesh": round6(symmetry_mesh) if symmetry_mesh is not None else 0.0,
        "framing": round6(framing),
        "insight_det": round6(insight_det),
        "front_pose_bonus": round6(front_pose_bonus),
        "side_profile_bonus": round6(side_profile_bonus),
        "view_front": 1.0 if view == "front" else 0.0,
        "view_side": 1.0 if view == "side" else 0.0,
        "insightface_available": 1.0 if INSIGHTFACE_AVAILABLE else 0.0,
        "mediapipe_available": 1.0 if MEDIAPIPE_AVAILABLE else 0.0,
    }

    if insight is not None:
        for key, value in insight.items():
            signals[key] = round6(value)

    return {
        "score": round6(score),
        "confidence": round6(confidence),
        "signals": signals,
    }


def fallback_payload(reason: str) -> Dict[str, Any]:
    return {
        "score": 0.50,
        "confidence": 0.0,
        "signals": {
            "fallback": 1.0,
            "reason_" + reason: 1.0,
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True)
    parser.add_argument("--view", default="front")
    args = parser.parse_args()

    image = cv2.imread(args.image)
    if image is None or image.size == 0:
        payload = fallback_payload("read_error")
        sys.stdout.write(json.dumps(payload))
        return 0

    view = "side" if str(args.view).lower() == "side" else "front"
    payload = compute_score(image, view)
    sys.stdout.write(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
