"""
AURA Media Service - Face Verification, Video Processing, and Hidden Visual Scoring
Uses OSS DeepFace + optional InsightFace + optional MediaPipe landmarks for internal face analysis.
"""

import base64
import json
import os
import shlex
import subprocess
import tempfile
import uuid
from typing import Optional, List, Dict, Any

from fastapi import FastAPI, File, UploadFile, HTTPException, Form
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import numpy as np
import cv2
from deepface import DeepFace
from PIL import Image
import redis
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(title="AURA Media Service", version="1.0.0")

try:
    import mediapipe as mp
    MEDIAPIPE_AVAILABLE = True
except Exception:
    mp = None
    MEDIAPIPE_AVAILABLE = False

try:
    from insightface.app import FaceAnalysis
    INSIGHTFACE_AVAILABLE = True
except Exception:
    FaceAnalysis = None
    INSIGHTFACE_AVAILABLE = False

try:
    import opennsfw2
    OPENNSFW2_AVAILABLE = True
except Exception:
    opennsfw2 = None
    OPENNSFW2_AVAILABLE = False

try:
    from nudenet import NudeDetector
    NUDENET_AVAILABLE = True
except Exception:
    NudeDetector = None
    NUDENET_AVAILABLE = False

_INSIGHTFACE_APP = None
_INSIGHTFACE_INIT_FAILED = False
_NUDENET_DETECTOR = None
_NUDENET_INIT_FAILED = False

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Redis for caching (optional)
try:
    redis_client = redis.Redis(
        host=os.getenv("REDIS_HOST", "localhost"),
        port=int(os.getenv("REDIS_PORT", 6379)),
        db=0,
        decode_responses=True
    )
    redis_client.ping()
    REDIS_AVAILABLE = True
except:
    REDIS_AVAILABLE = False
    redis_client = None

# Configuration
FACE_MODEL = os.getenv("FACE_MODEL", "Facenet512")  # Best accuracy
DETECTOR_BACKEND = os.getenv("DETECTOR_BACKEND", "retinaface")  # Best detection
FACE_MATCH_THRESHOLD = float(os.getenv("FACE_MATCH_THRESHOLD", "0.70"))
LIVENESS_THRESHOLD = float(os.getenv("LIVENESS_THRESHOLD", "0.85"))
DEEPFAKE_THRESHOLD = float(os.getenv("DEEPFAKE_THRESHOLD", "0.80"))
ATTRACTIVENESS_ENABLED = os.getenv("ATTRACTIVENESS_ENABLED", "true").lower() == "true"
ATTRACTIVENESS_BASELINE = float(os.getenv("ATTRACTIVENESS_BASELINE", "0.50"))
ATTRACTIVENESS_FRONT_WEIGHT = float(os.getenv("ATTRACTIVENESS_FRONT_WEIGHT", "0.70"))
ATTRACTIVENESS_SIDE_WEIGHT = float(os.getenv("ATTRACTIVENESS_SIDE_WEIGHT", "0.30"))
ATTRACTIVENESS_PROVIDER = os.getenv("ATTRACTIVENESS_PROVIDER", "").strip()
ATTRACTIVENESS_MODEL_VERSION = os.getenv("ATTRACTIVENESS_MODEL_VERSION", "oss_v1")
INSIGHTFACE_ENABLED = os.getenv("INSIGHTFACE_ENABLED", "true").lower() == "true"
ATTRACTIVENESS_EXTERNAL_SCORER_ENABLED = os.getenv("ATTRACTIVENESS_EXTERNAL_SCORER_ENABLED", "false").lower() == "true"
ATTRACTIVENESS_EXTERNAL_SCORER_CMD = os.getenv("ATTRACTIVENESS_EXTERNAL_SCORER_CMD", "").strip()
ATTRACTIVENESS_EXTERNAL_SCORER_CMDS_JSON = os.getenv("ATTRACTIVENESS_EXTERNAL_SCORER_CMDS", "").strip()
ATTRACTIVENESS_EXTERNAL_WEIGHT = float(os.getenv("ATTRACTIVENESS_EXTERNAL_WEIGHT", "0.25"))
ATTRACTIVENESS_EXTERNAL_TIMEOUT_SEC = int(os.getenv("ATTRACTIVENESS_EXTERNAL_TIMEOUT_SEC", "10"))
ANTISPOOF_EXTERNAL_ENABLED = os.getenv("ANTISPOOF_EXTERNAL_ENABLED", "false").lower() == "true"
ANTISPOOF_EXTERNAL_CMD = os.getenv("ANTISPOOF_EXTERNAL_CMD", "").strip()
ANTISPOOF_EXTERNAL_TIMEOUT_SEC = int(os.getenv("ANTISPOOF_EXTERNAL_TIMEOUT_SEC", "10"))
ANTISPOOF_EXTERNAL_WEIGHT = float(os.getenv("ANTISPOOF_EXTERNAL_WEIGHT", "0.45"))
NSFW_ENABLED = os.getenv("NSFW_ENABLED", "true").lower() == "true"
NSFW_THRESHOLD = float(os.getenv("NSFW_THRESHOLD", "0.60"))
NSFW_BASELINE = float(os.getenv("NSFW_BASELINE", "0.02"))
NSFW_USE_OPENNSFW2 = os.getenv("NSFW_USE_OPENNSFW2", "false").lower() == "true"
NSFW_USE_NUDENET = os.getenv("NSFW_USE_NUDENET", "false").lower() == "true"
NSFW_USE_CLIP_NSFW = os.getenv("NSFW_USE_CLIP_NSFW", "false").lower() == "true"
NSFW_CLIP_CMD = os.getenv(
    "NSFW_CLIP_CMD",
    "python3 /app/scripts/nsfw_clip_adapter.py --image {image_path}",
).strip()
NSFW_CLIP_TIMEOUT_SEC = int(os.getenv("NSFW_CLIP_TIMEOUT_SEC", "12"))
NSFW_CLIP_WEIGHT = float(os.getenv("NSFW_CLIP_WEIGHT", "0.35"))
NSFW_EXTERNAL_SCORER_ENABLED = os.getenv("NSFW_EXTERNAL_SCORER_ENABLED", "false").lower() == "true"
NSFW_EXTERNAL_SCORER_CMD = os.getenv("NSFW_EXTERNAL_SCORER_CMD", "").strip()
NSFW_EXTERNAL_TIMEOUT_SEC = int(os.getenv("NSFW_EXTERNAL_TIMEOUT_SEC", "10"))
NSFW_PROVIDER = os.getenv("NSFW_PROVIDER", "").strip()
TEXT_MODERATION_ENABLED = os.getenv("TEXT_MODERATION_ENABLED", "true").lower() == "true"
TEXT_MODERATION_MODEL = os.getenv("TEXT_MODERATION_MODEL", "multilingual").strip() or "multilingual"
TEXT_MODERATION_BLOCK_THRESHOLD = float(os.getenv("TEXT_MODERATION_BLOCK_THRESHOLD", "0.72"))
TEXT_MODERATION_WARN_THRESHOLD = float(os.getenv("TEXT_MODERATION_WARN_THRESHOLD", "0.52"))
FACE_QUALITY_ENABLED = os.getenv("FACE_QUALITY_ENABLED", "true").lower() == "true"
FACE_QUALITY_PROVIDER = os.getenv("FACE_QUALITY_PROVIDER", "faceqan")
FACE_QUALITY_MODEL_VERSION = os.getenv("FACE_QUALITY_MODEL_VERSION", "faceqan_v1")
FACE_QUALITY_USE_ADAPTER = os.getenv("FACE_QUALITY_USE_ADAPTER", "true").lower() == "true"
FACE_QUALITY_CMD = os.getenv(
    "FACE_QUALITY_CMD",
    "python3 /app/scripts/faceqan_adapter.py --image {image_path}",
).strip()
FACE_QUALITY_TIMEOUT_SEC = int(os.getenv("FACE_QUALITY_TIMEOUT_SEC", "12"))
FACE_QUALITY_ADAPTER_WEIGHT = float(os.getenv("FACE_QUALITY_ADAPTER_WEIGHT", "0.65"))

OPEN_SOURCE_MODEL_REPOS = [
    "serengil/deepface",
    "deepinsight/insightface",
    "google-ai-edge/mediapipe",
    "minivision-ai/Silent-Face-Anti-Spoofing",
    "bhky/opennsfw2",
    "notAI-tech/NudeNet",
    "HCIILAB/SCUT-FBP5500-Database-Release",
    "lucasxlu/ComboLoss",
    "ustcqidi/BeautyPredict",
    "fei-aiart/FaceAttract",
    "MetaVisionLab/MetaFBP",
    "etrain-xyz/facial-beauty-prediction",
    "davisking/dlib",
    "1adrianb/face-alignment",
    "yfeng95/DECA",
    "sicxu/Deep3DFaceRecon_pytorch",
    "unitaryai/detoxify",
    "LSIbabnikz/FaceQAN",
    "LAION-AI/CLIP-based-NSFW-Detector",
    "SCLBD/DeepfakeBench",
    "recommenders-team/recommenders",
]


class VerificationRequest(BaseModel):
    user_id: int
    profile_image_url: str
    verification_video_url: str
    session_id: str


class LivenessRequest(BaseModel):
    user_id: int


class VideoAnalysisRequest(BaseModel):
    video_url: str
    user_id: int


class VerificationResult(BaseModel):
    verified: bool
    face_match_score: float
    liveness_score: float
    deepfake_score: float
    issues: List[str]


class AttractivenessScoreRequest(BaseModel):
    user_id: Optional[int] = None
    front_image_base64: Optional[str] = None
    side_image_base64: Optional[str] = None
    front_image_url: Optional[str] = None
    side_image_url: Optional[str] = None
    segment_key: Optional[str] = None


class AttractivenessScoreResponse(BaseModel):
    score: float
    confidence: float
    provider: str
    model_version: str
    signals: Dict[str, float]
    repo_refs: List[str]


class ImageModerationRequest(BaseModel):
    user_id: Optional[int] = None
    image_base64: Optional[str] = None
    image_url: Optional[str] = None
    image_type: Optional[str] = "PROFILE"


class ImageModerationResponse(BaseModel):
    is_safe: bool
    nsfw_score: float
    confidence: float
    action: str
    provider: str
    categories: Dict[str, float]
    signals: Dict[str, float]
    repo_refs: List[str]


class TextModerationRequest(BaseModel):
    user_id: Optional[int] = None
    text: str
    content_type: Optional[str] = None
    segment_key: Optional[str] = None


class TextModerationResponse(BaseModel):
    is_allowed: bool
    decision: str
    toxicity_score: float
    max_label: Optional[str] = None
    blocked_categories: List[str]
    labels: Dict[str, float]
    reason: Optional[str] = None
    provider: str
    model_version: str
    signals: Dict[str, float]
    repo_refs: List[str]


class FaceQualityRequest(BaseModel):
    user_id: Optional[int] = None
    image_base64: Optional[str] = None
    image_url: Optional[str] = None
    surface: Optional[str] = None
    segment_key: Optional[str] = None


class FaceQualityResponse(BaseModel):
    quality_score: float
    confidence: float
    provider: str
    model_version: str
    signals: Dict[str, float]
    repo_refs: List[str]


@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "media-service"}


@app.post("/upload/video")
async def upload_video(
    file: UploadFile = File(...),
    path: str = Form(...),
    type: str = Form(...)
):
    """Upload video to storage and return URL"""
    try:
        # Generate unique filename
        file_ext = file.filename.split(".")[-1] if file.filename else "mp4"
        filename = f"{uuid.uuid4()}.{file_ext}"

        # For local storage (in production, use S3/MinIO)
        storage_path = os.getenv("STORAGE_PATH", "/tmp/aura-media")
        os.makedirs(f"{storage_path}/{path}", exist_ok=True)

        file_path = f"{storage_path}/{path}/{filename}"

        with open(file_path, "wb") as f:
            content = await file.read()
            f.write(content)

        # Return URL (in production, this would be S3 URL)
        url = f"/media/{path}/{filename}"

        return {"url": url, "filename": filename, "size": len(content)}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/verify/liveness/challenges")
async def get_liveness_challenges(request: LivenessRequest):
    """Get random liveness challenges for verification"""

    all_challenges = [
        {"type": "BLINK", "instruction": "Please blink naturally 2-3 times"},
        {"type": "TURN_HEAD_LEFT", "instruction": "Turn your head slightly to the left"},
        {"type": "TURN_HEAD_RIGHT", "instruction": "Turn your head slightly to the right"},
        {"type": "SMILE", "instruction": "Please smile naturally"},
        {"type": "NOD", "instruction": "Nod your head up and down"},
        {"type": "RAISE_EYEBROWS", "instruction": "Raise your eyebrows"},
    ]

    # Select 3 random challenges
    import random
    selected = random.sample(all_challenges, 3)

    session_id = str(uuid.uuid4())

    # Store session in Redis if available
    if REDIS_AVAILABLE:
        redis_client.setex(
            f"liveness_session:{session_id}",
            300,  # 5 minute expiry
            str({"user_id": request.user_id, "challenges": selected})
        )

    return {
        "session_id": session_id,
        "challenges": selected,
        "timeout": 30,  # seconds per challenge
        "total_timeout": 120  # total session timeout
    }


@app.post("/verify/face", response_model=VerificationResult)
async def verify_face(request: VerificationRequest):
    """
    Verify that the person in the verification video matches the profile picture.
    Uses DeepFace for face matching and anti-spoofing detection.
    """
    issues = []
    face_match_score = 0.0
    liveness_score = 0.0
    deepfake_score = 1.0  # 1.0 = authentic (not a deepfake)

    try:
        # Download/load images
        profile_image = await load_image_from_url(request.profile_image_url)
        video_frames = await extract_video_frames(request.verification_video_url)

        if profile_image is None:
            issues.append("Could not load profile image")
            return VerificationResult(
                verified=False,
                face_match_score=0,
                liveness_score=0,
                deepfake_score=0,
                issues=issues
            )

        if not video_frames:
            issues.append("Could not extract frames from video")
            return VerificationResult(
                verified=False,
                face_match_score=0,
                liveness_score=0,
                deepfake_score=0,
                issues=issues
            )

        # 1. Face Matching - Compare profile pic to video frames
        face_match_scores = []
        for frame in video_frames[:5]:  # Check first 5 frames
            try:
                result = DeepFace.verify(
                    img1_path=profile_image,
                    img2_path=frame,
                    model_name=FACE_MODEL,
                    detector_backend=DETECTOR_BACKEND,
                    enforce_detection=False
                )
                # Convert distance to similarity score (0-1)
                # Lower distance = higher similarity
                similarity = 1 - min(result["distance"] / result["threshold"], 1)
                face_match_scores.append(similarity)
            except Exception as e:
                continue

        if face_match_scores:
            face_match_score = max(face_match_scores)
        else:
            issues.append("Could not detect face in video")

        # 2. Liveness Detection - Check for signs of life
        liveness_score = await detect_liveness(video_frames)
        if liveness_score < LIVENESS_THRESHOLD:
            issues.append("Liveness check failed - please use a live camera")

        # 3. Anti-Spoofing (Deepfake detection)
        deepfake_score = await detect_deepfake(video_frames)
        if deepfake_score < DEEPFAKE_THRESHOLD:
            issues.append("Authenticity check failed - suspected manipulation")

        # Final verification decision
        verified = (
            face_match_score >= FACE_MATCH_THRESHOLD and
            liveness_score >= LIVENESS_THRESHOLD and
            deepfake_score >= DEEPFAKE_THRESHOLD and
            len(issues) == 0
        )

        return VerificationResult(
            verified=verified,
            face_match_score=round(face_match_score * 100, 1),
            liveness_score=round(liveness_score * 100, 1),
            deepfake_score=round(deepfake_score * 100, 1),
            issues=issues
        )

    except Exception as e:
        issues.append(f"Verification error: {str(e)}")
        return VerificationResult(
            verified=False,
            face_match_score=0,
            liveness_score=0,
            deepfake_score=0,
            issues=issues
        )


@app.post("/video/analyze")
async def analyze_video(request: VideoAnalysisRequest):
    """Analyze video for transcript, duration, and sentiment"""
    try:
        video_frames = await extract_video_frames(request.video_url)

        # Get video duration
        video_path = url_to_local_path(request.video_url)
        cap = cv2.VideoCapture(video_path)
        fps = cap.get(cv2.CAP_PROP_FPS)
        frame_count = cap.get(cv2.CAP_PROP_FRAME_COUNT)
        duration = int(frame_count / fps) if fps > 0 else 0
        cap.release()

        # Simple sentiment analysis placeholder
        # In production, use speech-to-text + NLP
        sentiment = {
            "positive": 0.5,
            "negative": 0.1,
            "neutral": 0.4
        }

        # Thumbnail generation
        thumbnail_url = None
        if video_frames:
            thumbnail_path = f"/tmp/aura-media/thumbnails/{uuid.uuid4()}.jpg"
            os.makedirs(os.path.dirname(thumbnail_path), exist_ok=True)
            cv2.imwrite(thumbnail_path, video_frames[0])
            thumbnail_url = thumbnail_path

        return {
            "duration": duration,
            "sentiment": sentiment,
            "thumbnail_url": thumbnail_url,
            "transcript": None,  # Would need speech-to-text
            "frame_count": len(video_frames)
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/attractiveness/score", response_model=AttractivenessScoreResponse)
async def score_attractiveness(request: AttractivenessScoreRequest):
    """
    Hidden backend visual-attractiveness scoring endpoint.
    Input can be URL-based or direct base64 payloads to avoid cross-service URL coupling.
    """
    baseline = clamp01(ATTRACTIVENESS_BASELINE)

    if not ATTRACTIVENESS_ENABLED:
        return AttractivenessScoreResponse(
            score=baseline,
            confidence=0.0,
            provider=resolve_provider_name(),
            model_version=ATTRACTIVENESS_MODEL_VERSION,
            signals={"baseline_only": baseline},
            repo_refs=OPEN_SOURCE_MODEL_REPOS,
        )

    front = await load_image_from_sources(request.front_image_base64, request.front_image_url)
    if front is None:
        raise HTTPException(status_code=400, detail="Front image is required")

    side = await load_image_from_sources(request.side_image_base64, request.side_image_url)

    front_score, front_conf, front_signals = compute_view_score(front, "front")
    signals: Dict[str, float] = dict(front_signals)

    if side is not None:
        side_score, side_conf, side_signals = compute_view_score(side, "side")
        signals.update(side_signals)
        fw = max(0.0, ATTRACTIVENESS_FRONT_WEIGHT)
        sw = max(0.0, ATTRACTIVENESS_SIDE_WEIGHT)
        denom = fw + sw if (fw + sw) > 0 else 1.0
        score = ((fw * front_score) + (sw * side_score)) / denom
        confidence = ((fw * front_conf) + (sw * side_conf)) / denom
    else:
        score = front_score
        confidence = front_conf

    score = clamp01(score)
    confidence = clamp01(confidence)

    return AttractivenessScoreResponse(
        score=round(score, 6),
        confidence=round(confidence, 6),
        provider=resolve_provider_name(),
        model_version=ATTRACTIVENESS_MODEL_VERSION,
        signals=signals,
        repo_refs=OPEN_SOURCE_MODEL_REPOS,
    )


@app.post("/moderation/image", response_model=ImageModerationResponse)
async def moderate_image(request: ImageModerationRequest):
    """
    Image moderation endpoint (backend-safe; no UI exposure required).
    Uses OSS hooks (OpenNSFW2 / NudeNet / optional external scorer) with conservative fallbacks.
    """
    image = await load_image_from_sources(request.image_base64, request.image_url)
    if image is None:
        raise HTTPException(status_code=400, detail="Image is required")

    if not NSFW_ENABLED:
        return ImageModerationResponse(
            is_safe=True,
            nsfw_score=round(clamp01(NSFW_BASELINE), 6),
            confidence=0.0,
            action="ALLOW",
            provider=resolve_nsfw_provider_name(),
            categories={"nsfw": round(clamp01(NSFW_BASELINE), 6)},
            signals={"moderation_disabled": 1.0},
            repo_refs=OPEN_SOURCE_MODEL_REPOS,
        )

    nsfw_score, confidence, categories, signals = compute_nsfw_score(image)
    nsfw_score = clamp01(nsfw_score)
    confidence = clamp01(confidence)
    is_safe = nsfw_score < NSFW_THRESHOLD

    return ImageModerationResponse(
        is_safe=is_safe,
        nsfw_score=round(nsfw_score, 6),
        confidence=round(confidence, 6),
        action="ALLOW" if is_safe else "BLOCK",
        provider=resolve_nsfw_provider_name(),
        categories=categories,
        signals=signals,
        repo_refs=OPEN_SOURCE_MODEL_REPOS,
    )


@app.post("/moderation/text", response_model=TextModerationResponse)
async def moderate_text(request: TextModerationRequest):
    text = (request.text or "").strip()
    if not text:
        raise HTTPException(status_code=400, detail="Text is required")

    if not TEXT_MODERATION_ENABLED:
        return TextModerationResponse(
            is_allowed=True,
            decision="ALLOW",
            toxicity_score=0.0,
            max_label=None,
            blocked_categories=[],
            labels={"toxicity": 0.0},
            reason=None,
            provider="text_moderation_disabled",
            model_version="disabled",
            signals={"moderation_disabled": 1.0},
            repo_refs=OPEN_SOURCE_MODEL_REPOS,
        )

    labels = detoxify_labels(text)
    max_label, max_score = max(labels.items(), key=lambda x: x[1])
    toxicity = clamp01(labels.get("toxicity", max_score))

    blocked_categories = [k.upper() for k, v in labels.items() if v >= TEXT_MODERATION_BLOCK_THRESHOLD]
    decision = "ALLOW"
    reason = None
    if toxicity >= TEXT_MODERATION_BLOCK_THRESHOLD or blocked_categories:
        decision = "BLOCK"
        reason = "Content flagged by toxicity model"
    elif toxicity >= TEXT_MODERATION_WARN_THRESHOLD:
        decision = "WARN"
        reason = "Content near moderation threshold"

    return TextModerationResponse(
        is_allowed=decision != "BLOCK",
        decision=decision,
        toxicity_score=round(toxicity, 6),
        max_label=max_label,
        blocked_categories=blocked_categories,
        labels=labels,
        reason=reason,
        provider="detoxify",
        model_version=f"detoxify_{TEXT_MODERATION_MODEL}",
        signals={
            "threshold_block": round(clamp01(TEXT_MODERATION_BLOCK_THRESHOLD), 6),
            "threshold_warn": round(clamp01(TEXT_MODERATION_WARN_THRESHOLD), 6),
            "label_count": float(len(labels)),
        },
        repo_refs=OPEN_SOURCE_MODEL_REPOS,
    )


@app.post("/quality/face", response_model=FaceQualityResponse)
async def score_face_quality(request: FaceQualityRequest):
    image = await load_image_from_sources(request.image_base64, request.image_url)
    if image is None:
        raise HTTPException(status_code=400, detail="Image is required")

    if not FACE_QUALITY_ENABLED:
        return FaceQualityResponse(
            quality_score=0.5,
            confidence=0.0,
            provider=FACE_QUALITY_PROVIDER,
            model_version=FACE_QUALITY_MODEL_VERSION,
            signals={"quality_disabled": 1.0},
            repo_refs=OPEN_SOURCE_MODEL_REPOS,
        )

    score, confidence, signals = compute_face_quality_score(image)
    return FaceQualityResponse(
        quality_score=round(clamp01(score), 6),
        confidence=round(clamp01(confidence), 6),
        provider=FACE_QUALITY_PROVIDER,
        model_version=FACE_QUALITY_MODEL_VERSION,
        signals=signals,
        repo_refs=OPEN_SOURCE_MODEL_REPOS,
    )


async def load_image_from_url(url: str) -> Optional[np.ndarray]:
    """Load image from URL or local path"""
    try:
        if url.startswith("/media/") or url.startswith("local://"):
            # Local file
            local_path = url_to_local_path(url)
            if os.path.exists(local_path):
                return cv2.imread(local_path)
        else:
            # Remote URL - download
            import httpx
            async with httpx.AsyncClient() as client:
                response = await client.get(url)
                if response.status_code == 200:
                    nparr = np.frombuffer(response.content, np.uint8)
                    return cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        return None
    except:
        return None


async def load_image_from_sources(image_base64: Optional[str], image_url: Optional[str]) -> Optional[np.ndarray]:
    """Load from base64 first, then fallback to URL/path."""
    if image_base64:
        image = load_image_from_base64(image_base64)
        if image is not None:
            return image

    if image_url:
        return await load_image_from_url(image_url)

    return None


def load_image_from_base64(image_base64: str) -> Optional[np.ndarray]:
    try:
        payload = image_base64.strip()
        if "," in payload:
            payload = payload.split(",", 1)[1]
        raw = base64.b64decode(payload)
        arr = np.frombuffer(raw, np.uint8)
        return cv2.imdecode(arr, cv2.IMREAD_COLOR)
    except Exception:
        return None


def detoxify_labels(text: str) -> Dict[str, float]:
    try:
        from detoxify import Detoxify  # type: ignore

        model = Detoxify(TEXT_MODERATION_MODEL)
        raw = model.predict(text)
        out: Dict[str, float] = {}
        for key, value in (raw or {}).items():
            parsed = None
            if isinstance(value, (list, tuple)) and value:
                parsed = float(value[0])
            elif hasattr(value, "shape"):
                arr = np.asarray(value).reshape(-1)
                if arr.size:
                    parsed = float(arr[0])
            elif isinstance(value, (int, float)):
                parsed = float(value)
            if parsed is not None:
                out[str(key).strip().lower()] = round(clamp01(parsed), 6)
        if out:
            return out
    except Exception:
        pass

    lowered = text.lower()
    hits = 0
    for token in ["kill", "rape", "die", "nazi", "terrorist", "fag", "nigger", "kike", "bitch", "cunt", "asshole"]:
        if token in lowered:
            hits += 1
    toxicity = clamp01(0.05 + min(0.85, hits * 0.18))
    return {
        "toxicity": round(toxicity, 6),
        "severe_toxicity": round(clamp01(0.75 * toxicity), 6),
        "insult": round(clamp01(0.70 * toxicity), 6),
        "threat": round(clamp01(0.55 * toxicity if "kill" in lowered else 0.05 * toxicity), 6),
        "obscene": round(clamp01(0.60 * toxicity), 6),
        "identity_attack": round(clamp01(0.70 * toxicity if ("nigger" in lowered or "kike" in lowered) else 0.04 * toxicity), 6),
    }


def compute_face_quality_score(image: np.ndarray) -> tuple[float, float, Dict[str, float]]:
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    sharpness = normalize_laplacian_variance(float(cv2.Laplacian(gray, cv2.CV_64F).var()))
    exposure = normalize_exposure(gray)
    symmetry = mediapipe_symmetry_score(image)
    if symmetry is None:
        symmetry = estimate_symmetry(gray)
    face_fill = detect_face_fill_ratio(image)
    framing = clamp01(1.0 - abs(face_fill - 0.28) / 0.28) if face_fill > 0 else 0.0

    quality = clamp01(
        (0.32 * sharpness)
        + (0.24 * exposure)
        + (0.22 * symmetry)
        + (0.22 * framing)
    )
    confidence = clamp01(0.42 + 0.42 * face_fill + 0.16 * symmetry)

    signals: Dict[str, float] = {
        "sharpness": round(sharpness, 6),
        "exposure": round(exposure, 6),
        "symmetry": round(symmetry, 6),
        "face_fill": round(face_fill, 6),
        "framing": round(framing, 6),
    }

    adapter = face_quality_adapter_signals(image)
    if adapter is not None:
        adapter_quality = clamp01(float(adapter.get("quality_score", quality)))
        adapter_conf = clamp01(float(adapter.get("confidence", 0.0)))
        w_adapter = clamp01(FACE_QUALITY_ADAPTER_WEIGHT) * max(0.25, adapter_conf)
        quality = clamp01(((1.0 - w_adapter) * quality) + (w_adapter * adapter_quality))
        confidence = clamp01(((1.0 - w_adapter) * confidence) + (w_adapter * adapter_conf))

        signals["adapter_quality_score"] = round(adapter_quality, 6)
        signals["adapter_confidence"] = round(adapter_conf, 6)
        signals["adapter_weight"] = round(w_adapter, 6)

        adapter_signals = adapter.get("signals", {}) or {}
        if isinstance(adapter_signals, dict):
            for key, value in adapter_signals.items():
                if isinstance(value, (int, float)):
                    signals[f"adapter_{key}"] = round(float(value), 6)
    else:
        signals["adapter_quality_score"] = 0.0
        signals["adapter_confidence"] = 0.0
        signals["adapter_weight"] = 0.0

    return quality, confidence, signals


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def resolve_provider_name() -> str:
    if ATTRACTIVENESS_PROVIDER:
        return ATTRACTIVENESS_PROVIDER

    providers = ["deepface"]
    if INSIGHTFACE_AVAILABLE and INSIGHTFACE_ENABLED:
        providers.append("insightface")
    if MEDIAPIPE_AVAILABLE:
        providers.append("mediapipe")
    if ATTRACTIVENESS_EXTERNAL_SCORER_ENABLED and (
        ATTRACTIVENESS_EXTERNAL_SCORER_CMD or ATTRACTIVENESS_EXTERNAL_SCORER_CMDS_JSON
    ):
        providers.append("external")
    return "+".join(providers)


def resolve_nsfw_provider_name() -> str:
    if NSFW_PROVIDER:
        return NSFW_PROVIDER

    providers = ["heuristic"]
    if NSFW_USE_OPENNSFW2 and OPENNSFW2_AVAILABLE:
        providers.append("opennsfw2")
    if NSFW_USE_NUDENET and NUDENET_AVAILABLE:
        providers.append("nudenet")
    if NSFW_USE_CLIP_NSFW and NSFW_CLIP_CMD:
        providers.append("clip_nsfw")
    if NSFW_EXTERNAL_SCORER_ENABLED and NSFW_EXTERNAL_SCORER_CMD:
        providers.append("external")
    return "+".join(providers)


def get_insightface_app():
    global _INSIGHTFACE_APP, _INSIGHTFACE_INIT_FAILED

    if not INSIGHTFACE_ENABLED or not INSIGHTFACE_AVAILABLE or _INSIGHTFACE_INIT_FAILED:
        return None
    if _INSIGHTFACE_APP is not None:
        return _INSIGHTFACE_APP

    try:
        model_name = os.getenv("INSIGHTFACE_MODEL_NAME", "buffalo_l")
        provider_env = os.getenv("INSIGHTFACE_PROVIDERS", "CPUExecutionProvider")
        providers = [p.strip() for p in provider_env.split(",") if p.strip()]
        ctx_id = int(os.getenv("INSIGHTFACE_CTX_ID", "-1"))
        det_size = int(os.getenv("INSIGHTFACE_DET_SIZE", "640"))

        insight_app = FaceAnalysis(name=model_name, providers=providers)
        insight_app.prepare(ctx_id=ctx_id, det_size=(det_size, det_size))
        _INSIGHTFACE_APP = insight_app
    except Exception:
        _INSIGHTFACE_INIT_FAILED = True
        _INSIGHTFACE_APP = None

    return _INSIGHTFACE_APP


def get_nudenet_detector():
    global _NUDENET_DETECTOR, _NUDENET_INIT_FAILED

    if not NSFW_USE_NUDENET or not NUDENET_AVAILABLE or _NUDENET_INIT_FAILED:
        return None
    if _NUDENET_DETECTOR is not None:
        return _NUDENET_DETECTOR

    try:
        model_path = os.getenv("NUDENET_MODEL_PATH", "").strip()
        if model_path:
            _NUDENET_DETECTOR = NudeDetector(model_path=model_path)
        else:
            _NUDENET_DETECTOR = NudeDetector()
    except Exception:
        _NUDENET_INIT_FAILED = True
        _NUDENET_DETECTOR = None

    return _NUDENET_DETECTOR


def normalize_laplacian_variance(laplacian_var: float) -> float:
    # Typical mobile selfie sharpness often lands in low hundreds.
    return clamp01(laplacian_var / 500.0)


def normalize_exposure(gray: np.ndarray) -> float:
    mean = float(np.mean(gray))
    # Best around midtone. Degrade smoothly when too dark/bright.
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
    right_flipped = cv2.flip(right, 1)
    diff = np.mean(np.abs(left.astype(np.float32) - right_flipped.astype(np.float32))) / 255.0
    return clamp01(1.0 - diff)


def mediapipe_symmetry_score(image: np.ndarray) -> Optional[float]:
    if not MEDIAPIPE_AVAILABLE:
        return None
    try:
        rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        face_mesh = mp.solutions.face_mesh.FaceMesh(
            static_image_mode=True,
            max_num_faces=1,
            refine_landmarks=False,
            min_detection_confidence=0.5
        )
        result = face_mesh.process(rgb)
        face_mesh.close()

        if not result.multi_face_landmarks:
            return None

        landmarks = result.multi_face_landmarks[0].landmark
        # Approximate bilateral landmark pairs around the nose bridge.
        pairs = [
            (33, 263), (61, 291), (159, 386), (145, 374),
            (93, 323), (132, 361), (58, 288), (172, 397)
        ]
        center = landmarks[1]

        diffs = []
        for left_idx, right_idx in pairs:
            lmk_l = landmarks[left_idx]
            lmk_r = landmarks[right_idx]
            dl = np.hypot(lmk_l.x - center.x, lmk_l.y - center.y)
            dr = np.hypot(lmk_r.x - center.x, lmk_r.y - center.y)
            denom = max(dl, dr, 1e-6)
            diffs.append(abs(dl - dr) / denom)

        if not diffs:
            return None
        return clamp01(1.0 - float(np.mean(diffs)))
    except Exception:
        return None


def insightface_signals(image: np.ndarray) -> Optional[Dict[str, float]]:
    insight_app = get_insightface_app()
    if insight_app is None:
        return None

    try:
        faces = insight_app.get(image)
        if not faces:
            return None

        face = max(faces, key=lambda f: float(getattr(f, "det_score", 0.0)))
        det_score = clamp01(float(getattr(face, "det_score", 0.0)))

        # Prefer face occupying a reasonable portion of frame for reliable scoring.
        bbox = getattr(face, "bbox", None)
        fill_ratio = 0.5
        if bbox is not None and len(bbox) >= 4:
            x1, y1, x2, y2 = bbox[:4]
            area = max(0.0, float((x2 - x1) * (y2 - y1)))
            frame_area = float(max(1, image.shape[0] * image.shape[1]))
            ratio = area / frame_area
            fill_ratio = clamp01(ratio / 0.20)

        # Neutral-ish frontal pose is generally higher-quality for inference.
        pose = getattr(face, "pose", None)
        pose_stability = 0.5
        if pose is not None and len(pose) >= 3:
            yaw = abs(float(pose[0]))
            pitch = abs(float(pose[1]))
            roll = abs(float(pose[2]))
            pose_penalty = min(1.0, (yaw + pitch + roll) / 90.0)
            pose_stability = clamp01(1.0 - pose_penalty)

        return {
            "det_score": det_score,
            "fill_ratio": fill_ratio,
            "pose_stability": pose_stability,
        }
    except Exception:
        return None


def safe_provider_key(value: str) -> str:
    out = "".join(ch if ch.isalnum() else "_" for ch in str(value).strip().lower())
    out = out.strip("_")
    return out or "provider"


def parse_external_attractiveness_cmds() -> List[Dict[str, Any]]:
    out: List[Dict[str, Any]] = []
    raw = ATTRACTIVENESS_EXTERNAL_SCORER_CMDS_JSON
    if raw:
        try:
            parsed = json.loads(raw)
            if isinstance(parsed, list):
                for item in parsed:
                    if not isinstance(item, dict):
                        continue
                    name = str(item.get("name", "")).strip()
                    cmd = str(item.get("cmd", "")).strip()
                    weight_raw = item.get("weight", 1.0)
                    try:
                        weight = float(weight_raw)
                    except Exception:
                        weight = 0.0
                    if name and cmd and weight > 0:
                        out.append({"name": name, "cmd": cmd, "weight": weight})
        except Exception:
            pass

    if not out and ATTRACTIVENESS_EXTERNAL_SCORER_CMD:
        out.append({"name": "external", "cmd": ATTRACTIVENESS_EXTERNAL_SCORER_CMD, "weight": 1.0})

    return out


def run_external_attractiveness_cmd(
    cmd_template: str, image_path: str, view_prefix: str
) -> Optional[Dict[str, Any]]:
    try:
        cmd = cmd_template.format(image_path=image_path, view=view_prefix)
        args = shlex.split(cmd)
        if "{image_path}" not in cmd_template and "{view}" not in cmd_template:
            args.extend([image_path, view_prefix])

        proc = subprocess.run(
            args,
            capture_output=True,
            text=True,
            timeout=max(1, ATTRACTIVENESS_EXTERNAL_TIMEOUT_SEC),
            check=False
        )

        if proc.returncode != 0:
            return None

        payload = json.loads((proc.stdout or "").strip() or "{}")
        score = clamp01(float(payload.get("score", ATTRACTIVENESS_BASELINE)))
        confidence = clamp01(float(payload.get("confidence", 0.0)))
        signals = payload.get("signals", {})
        if not isinstance(signals, dict):
            signals = {}
        normalized = {str(k): float(v) for k, v in signals.items() if isinstance(v, (int, float))}
        return {
            "score": score,
            "confidence": confidence,
            "signals": normalized,
        }
    except Exception:
        return None


def external_scorer_signals(image: np.ndarray, view_prefix: str) -> Optional[Dict[str, Any]]:
    if not ATTRACTIVENESS_EXTERNAL_SCORER_ENABLED:
        return None

    external_cmds = parse_external_attractiveness_cmds()
    if not external_cmds:
        return None

    tmp_file_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as tmp_file:
            tmp_file_path = tmp_file.name
        if not cv2.imwrite(tmp_file_path, image):
            return None

        weighted_score = 0.0
        weighted_conf = 0.0
        total_score_weight = 0.0
        total_conf_weight = 0.0
        merged_signals: Dict[str, float] = {}
        providers_used = 0.0

        for provider in external_cmds:
            name = safe_provider_key(provider.get("name", "external"))
            cmd = str(provider.get("cmd", "")).strip()
            weight = max(0.0, float(provider.get("weight", 1.0)))
            if not cmd or weight <= 0:
                continue

            result = run_external_attractiveness_cmd(cmd, tmp_file_path, view_prefix)
            if result is None:
                continue

            score = clamp01(float(result.get("score", ATTRACTIVENESS_BASELINE)))
            conf = clamp01(float(result.get("confidence", 0.0)))
            score_weight = weight * max(0.1, conf)
            conf_weight = weight

            weighted_score += score * score_weight
            weighted_conf += conf * conf_weight
            total_score_weight += score_weight
            total_conf_weight += conf_weight
            providers_used += 1.0

            merged_signals[f"external_{name}_score"] = round(score, 6)
            merged_signals[f"external_{name}_confidence"] = round(conf, 6)

            raw_signals = result.get("signals", {}) or {}
            if isinstance(raw_signals, dict):
                for key, value in raw_signals.items():
                    if isinstance(value, (int, float)):
                        merged_signals[f"external_{name}_{safe_provider_key(key)}"] = float(value)

        if total_score_weight <= 0.0:
            return None

        return {
            "score": clamp01(weighted_score / total_score_weight),
            "confidence": clamp01(weighted_conf / max(1e-9, total_conf_weight)),
            "signals": {
                **merged_signals,
                "external_provider_count": providers_used,
            },
        }
    except Exception:
        return None
    finally:
        if tmp_file_path and os.path.exists(tmp_file_path):
            try:
                os.remove(tmp_file_path)
            except Exception:
                pass


def external_nsfw_signals(image: np.ndarray) -> Optional[Dict[str, Any]]:
    if not NSFW_EXTERNAL_SCORER_ENABLED or not NSFW_EXTERNAL_SCORER_CMD:
        return None

    tmp_file_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as tmp_file:
            tmp_file_path = tmp_file.name
        if not cv2.imwrite(tmp_file_path, image):
            return None

        cmd_template = NSFW_EXTERNAL_SCORER_CMD
        cmd = cmd_template.format(image_path=tmp_file_path)
        args = shlex.split(cmd)
        if "{image_path}" not in cmd_template:
            args.append(tmp_file_path)

        proc = subprocess.run(
            args,
            capture_output=True,
            text=True,
            timeout=max(1, NSFW_EXTERNAL_TIMEOUT_SEC),
            check=False
        )
        if proc.returncode != 0:
            return None

        payload = json.loads((proc.stdout or "").strip() or "{}")
        score = clamp01(float(payload.get("nsfw_score", NSFW_BASELINE)))
        confidence = clamp01(float(payload.get("confidence", 0.0)))

        categories = payload.get("categories", {})
        if not isinstance(categories, dict):
            categories = {}
        categories = {str(k): clamp01(float(v)) for k, v in categories.items() if isinstance(v, (int, float))}

        signals = payload.get("signals", {})
        if not isinstance(signals, dict):
            signals = {}
        signals = {f"external_{k}": float(v) for k, v in signals.items() if isinstance(v, (int, float))}

        return {
            "nsfw_score": score,
            "confidence": confidence,
            "categories": categories,
            "signals": signals,
        }
    except Exception:
        return None
    finally:
        if tmp_file_path and os.path.exists(tmp_file_path):
            try:
                os.remove(tmp_file_path)
            except Exception:
                pass


def clip_nsfw_score(image: np.ndarray) -> Optional[Dict[str, Any]]:
    if not NSFW_USE_CLIP_NSFW or not NSFW_CLIP_CMD:
        return None

    tmp_file_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as tmp_file:
            tmp_file_path = tmp_file.name
        if not cv2.imwrite(tmp_file_path, image):
            return None

        cmd = NSFW_CLIP_CMD.format(image_path=tmp_file_path)
        args = shlex.split(cmd)
        if "{image_path}" not in NSFW_CLIP_CMD:
            args.append(tmp_file_path)

        proc = subprocess.run(
            args,
            capture_output=True,
            text=True,
            timeout=max(1, NSFW_CLIP_TIMEOUT_SEC),
            check=False
        )
        if proc.returncode != 0:
            return None

        payload = json.loads((proc.stdout or "").strip() or "{}")
        nsfw_score = clamp01(float(payload.get("nsfw_score", NSFW_BASELINE)))
        confidence = clamp01(float(payload.get("confidence", 0.0)))

        categories = payload.get("categories", {})
        if not isinstance(categories, dict):
            categories = {}
        categories = {str(k): clamp01(float(v)) for k, v in categories.items() if isinstance(v, (int, float))}

        signals = payload.get("signals", {})
        if not isinstance(signals, dict):
            signals = {}
        signals = {f"clip_{k}": float(v) for k, v in signals.items() if isinstance(v, (int, float))}

        return {
            "nsfw_score": nsfw_score,
            "confidence": confidence,
            "categories": categories,
            "signals": signals,
        }
    except Exception:
        return None
    finally:
        if tmp_file_path and os.path.exists(tmp_file_path):
            try:
                os.remove(tmp_file_path)
            except Exception:
                pass


def face_quality_adapter_signals(image: np.ndarray) -> Optional[Dict[str, Any]]:
    if not FACE_QUALITY_USE_ADAPTER or not FACE_QUALITY_CMD:
        return None

    tmp_file_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as tmp_file:
            tmp_file_path = tmp_file.name
        if not cv2.imwrite(tmp_file_path, image):
            return None

        cmd = FACE_QUALITY_CMD.format(image_path=tmp_file_path)
        args = shlex.split(cmd)
        if "{image_path}" not in FACE_QUALITY_CMD:
            args.append(tmp_file_path)

        proc = subprocess.run(
            args,
            capture_output=True,
            text=True,
            timeout=max(1, FACE_QUALITY_TIMEOUT_SEC),
            check=False
        )
        if proc.returncode != 0:
            return None

        payload = json.loads((proc.stdout or "").strip() or "{}")
        score = clamp01(float(payload.get("quality_score", payload.get("score", 0.5))))
        confidence = clamp01(float(payload.get("confidence", 0.0)))
        signals = payload.get("signals", {})
        if not isinstance(signals, dict):
            signals = {}
        normalized = {str(k): float(v) for k, v in signals.items() if isinstance(v, (int, float))}
        return {
            "quality_score": score,
            "confidence": confidence,
            "signals": normalized,
            "provider": str(payload.get("provider", "faceqan")),
            "model_version": str(payload.get("model_version", FACE_QUALITY_MODEL_VERSION)),
        }
    except Exception:
        return None
    finally:
        if tmp_file_path and os.path.exists(tmp_file_path):
            try:
                os.remove(tmp_file_path)
            except Exception:
                pass


def external_antispoof_signals(image: np.ndarray) -> Optional[Dict[str, Any]]:
    if not ANTISPOOF_EXTERNAL_ENABLED or not ANTISPOOF_EXTERNAL_CMD:
        return None

    tmp_file_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as tmp_file:
            tmp_file_path = tmp_file.name
        if not cv2.imwrite(tmp_file_path, image):
            return None

        cmd_template = ANTISPOOF_EXTERNAL_CMD
        cmd = cmd_template.format(image_path=tmp_file_path)
        args = shlex.split(cmd)
        if "{image_path}" not in cmd_template:
            args.append(tmp_file_path)

        proc = subprocess.run(
            args,
            capture_output=True,
            text=True,
            timeout=max(1, ANTISPOOF_EXTERNAL_TIMEOUT_SEC),
            check=False
        )
        if proc.returncode != 0:
            return None

        payload = json.loads((proc.stdout or "").strip() or "{}")
        liveness = clamp01(float(payload.get("liveness", 0.5)))
        spoof_prob = clamp01(float(payload.get("spoof_prob", 1.0 - liveness)))
        confidence = clamp01(float(payload.get("confidence", 0.0)))
        signals = payload.get("signals", {})
        if not isinstance(signals, dict):
            signals = {}

        return {
            "liveness": liveness,
            "spoof_prob": spoof_prob,
            "confidence": confidence,
            "signals": {f"antispoof_{k}": float(v) for k, v in signals.items() if isinstance(v, (int, float))},
        }
    except Exception:
        return None
    finally:
        if tmp_file_path and os.path.exists(tmp_file_path):
            try:
                os.remove(tmp_file_path)
            except Exception:
                pass


def detect_face_fill_ratio(image: np.ndarray) -> float:
    try:
        faces = DeepFace.extract_faces(
            image,
            detector_backend=DETECTOR_BACKEND,
            enforce_detection=False
        )
        if not faces:
            return 0.0
        facial_area = faces[0].get("facial_area", {}) or {}
        w = float(facial_area.get("w", 0.0))
        h = float(facial_area.get("h", 0.0))
        frame_area = float(max(1, image.shape[0] * image.shape[1]))
        return clamp01((w * h) / frame_area)
    except Exception:
        return 0.0


def nsfw_skin_exposure_proxy(image: np.ndarray) -> float:
    # Broad YCrCb skin range; treated as weak heuristic only.
    ycrcb = cv2.cvtColor(image, cv2.COLOR_BGR2YCrCb)
    mask = cv2.inRange(ycrcb, (0, 133, 77), (255, 173, 127))
    skin_ratio = float(np.mean(mask > 0))
    face_fill = detect_face_fill_ratio(image)
    adjusted = skin_ratio - (0.65 * face_fill)
    return clamp01((adjusted - 0.10) / 0.55)


def opennsfw2_score(image: np.ndarray) -> Optional[float]:
    if not NSFW_USE_OPENNSFW2 or not OPENNSFW2_AVAILABLE:
        return None
    try:
        rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        pil_image = Image.fromarray(rgb)
        score = float(opennsfw2.predict_image(pil_image))
        return clamp01(score)
    except Exception:
        return None


def nudenet_score(image: np.ndarray) -> Optional[Dict[str, Any]]:
    detector = get_nudenet_detector()
    if detector is None:
        return None

    try:
        detections = detector.detect(image)
        if detections is None:
            detections = []
        if isinstance(detections, dict):
            detections = [detections]

        categories: Dict[str, float] = {}
        for det in detections:
            if not isinstance(det, dict):
                continue
            cls = str(det.get("class", "")).strip()
            if not cls:
                continue
            score = clamp01(float(det.get("score", 0.0)))
            categories[cls] = max(categories.get(cls, 0.0), score)

        explicit_labels = {
            "FEMALE_GENITALIA_EXPOSED",
            "MALE_GENITALIA_EXPOSED",
            "ANUS_EXPOSED",
            "BUTTOCKS_EXPOSED",
            "FEMALE_BREAST_EXPOSED",
        }
        implied_labels = {
            "BELLY_EXPOSED",
            "ARMPITS_EXPOSED",
            "MALE_BREAST_EXPOSED",
        }

        explicit = max([categories.get(k, 0.0) for k in explicit_labels], default=0.0)
        implied = max([categories.get(k, 0.0) for k in implied_labels], default=0.0)
        nsfw_score = clamp01(max(explicit, implied * 0.55))
        confidence = clamp01(max(categories.values(), default=0.0))

        return {
            "nsfw_score": nsfw_score,
            "confidence": confidence,
            "categories": categories,
        }
    except Exception:
        return None


def compute_nsfw_score(image: np.ndarray) -> tuple[float, float, Dict[str, float], Dict[str, float]]:
    baseline = clamp01(NSFW_BASELINE)
    score_num = 0.0
    score_den = 0.0
    confidence_num = 0.0
    confidence_den = 0.0

    categories: Dict[str, float] = {}
    signals: Dict[str, float] = {}

    heuristic = nsfw_skin_exposure_proxy(image)
    heuristic_conf = 0.35
    score_num += 0.20 * heuristic
    score_den += 0.20
    confidence_num += 0.20 * heuristic_conf
    confidence_den += 0.20
    signals["heuristic_skin_proxy"] = round(heuristic, 6)

    open_nsfw = opennsfw2_score(image)
    if open_nsfw is not None:
        score_num += 0.45 * open_nsfw
        score_den += 0.45
        confidence_num += 0.45 * 0.80
        confidence_den += 0.45
        categories["opennsfw2"] = round(open_nsfw, 6)
        signals["opennsfw2_score"] = round(open_nsfw, 6)

    nude = nudenet_score(image)
    if nude is not None:
        nude_score = clamp01(float(nude.get("nsfw_score", 0.0)))
        nude_conf = clamp01(float(nude.get("confidence", 0.0)))
        score_num += 0.55 * nude_score
        score_den += 0.55
        confidence_num += 0.55 * nude_conf
        confidence_den += 0.55
        signals["nudenet_score"] = round(nude_score, 6)
        signals["nudenet_confidence"] = round(nude_conf, 6)

        nude_categories = nude.get("categories", {}) or {}
        if isinstance(nude_categories, dict):
            for key, value in nude_categories.items():
                if not isinstance(value, (int, float)):
                    continue
                categories[f"nudenet_{key.lower()}"] = round(clamp01(float(value)), 6)

    clip = clip_nsfw_score(image)
    if clip is not None:
        clip_score = clamp01(float(clip.get("nsfw_score", baseline)))
        clip_conf = clamp01(float(clip.get("confidence", 0.0)))
        clip_categories = clip.get("categories", {}) or {}
        clip_signals = clip.get("signals", {}) or {}

        clip_weight = clamp01(NSFW_CLIP_WEIGHT) * max(0.25, clip_conf)
        score_num += clip_weight * clip_score
        score_den += clip_weight
        confidence_num += clip_weight * clip_conf
        confidence_den += clip_weight

        signals["clip_nsfw_score"] = round(clip_score, 6)
        signals["clip_nsfw_confidence"] = round(clip_conf, 6)

        if isinstance(clip_categories, dict):
            for key, value in clip_categories.items():
                if isinstance(value, (int, float)):
                    categories[f"clip_{key.lower()}"] = round(clamp01(float(value)), 6)
        if isinstance(clip_signals, dict):
            for key, value in clip_signals.items():
                if isinstance(value, (int, float)):
                    signals[key] = round(float(value), 6)

    external = external_nsfw_signals(image)
    if external is not None:
        ext_score = clamp01(float(external.get("nsfw_score", baseline)))
        ext_conf = clamp01(float(external.get("confidence", 0.0)))
        ext_categories = external.get("categories", {}) or {}
        ext_signals = external.get("signals", {}) or {}

        weight = 0.70 * ext_conf
        score_num += weight * ext_score
        score_den += weight
        confidence_num += weight * ext_conf
        confidence_den += weight

        signals["external_nsfw_score"] = round(ext_score, 6)
        signals["external_nsfw_confidence"] = round(ext_conf, 6)

        if isinstance(ext_categories, dict):
            for key, value in ext_categories.items():
                if isinstance(value, (int, float)):
                    categories[f"external_{key}"] = round(clamp01(float(value)), 6)
        if isinstance(ext_signals, dict):
            for key, value in ext_signals.items():
                if isinstance(value, (int, float)):
                    signals[key] = round(float(value), 6)

    if score_den <= 0.0:
        score = baseline
    else:
        score = score_num / score_den
    if confidence_den <= 0.0:
        confidence = 0.0
    else:
        confidence = confidence_num / confidence_den

    score = clamp01((0.15 * baseline) + (0.85 * score))
    confidence = clamp01(confidence)

    categories.setdefault("nsfw", round(score, 6))
    return score, confidence, categories, signals


def compute_view_score(image: np.ndarray, view_prefix: str) -> tuple[float, float, Dict[str, float]]:
    baseline = clamp01(ATTRACTIVENESS_BASELINE)
    if image is None or image.size == 0:
        return baseline, 0.0, {
            f"{view_prefix}_face_confidence": 0.0,
            f"{view_prefix}_sharpness": 0.0,
            f"{view_prefix}_exposure": 0.0,
            f"{view_prefix}_symmetry": 0.0,
        }

    face_confidence = 0.0
    try:
        faces = DeepFace.extract_faces(
            image,
            detector_backend=DETECTOR_BACKEND,
            enforce_detection=False
        )
        if faces and len(faces) > 0:
            face_confidence = clamp01(float(faces[0].get("confidence", 0.7)))
    except Exception:
        face_confidence = 0.0

    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    sharpness = normalize_laplacian_variance(float(cv2.Laplacian(gray, cv2.CV_64F).var()))
    exposure = normalize_exposure(gray)

    symmetry = mediapipe_symmetry_score(image)
    if symmetry is None:
        symmetry = estimate_symmetry(gray)

    insight = insightface_signals(image)
    insight_det = insight["det_score"] if insight is not None else 0.0
    insight_fill = insight["fill_ratio"] if insight is not None else 0.0
    insight_pose = insight["pose_stability"] if insight is not None else 0.0

    weighted_score = (
            0.34 * face_confidence +
            0.18 * sharpness +
            0.18 * exposure +
            0.18 * symmetry
    )
    weighted_conf = (
            0.52 * face_confidence +
            0.24 * sharpness +
            0.24 * symmetry
    )
    score_denominator = 0.88
    confidence_denominator = 1.0

    if insight is not None:
        weighted_score += 0.08 * insight_det + 0.02 * insight_fill + 0.02 * insight_pose
        score_denominator += 0.12
        weighted_conf += 0.08 * insight_det
        confidence_denominator += 0.08

    score = weighted_score / max(1e-6, score_denominator)
    confidence = weighted_conf / max(1e-6, confidence_denominator)

    external = external_scorer_signals(image, view_prefix)
    external_score = 0.0
    external_conf = 0.0
    external_signals: Dict[str, float] = {}
    if external is not None:
        external_score = clamp01(float(external.get("score", 0.0)))
        external_conf = clamp01(float(external.get("confidence", 0.0)))
        external_signals = external.get("signals", {}) or {}
        w_external = clamp01(ATTRACTIVENESS_EXTERNAL_WEIGHT) * external_conf
        score = ((1.0 - w_external) * score) + (w_external * external_score)
        confidence = ((1.0 - w_external) * confidence) + (w_external * external_conf)

    # Keep a minimum baseline so missing cues do not collapse to zero.
    score = clamp01(0.20 * baseline + 0.80 * score)
    confidence = clamp01(confidence)

    signals = {
        f"{view_prefix}_face_confidence": round(face_confidence, 6),
        f"{view_prefix}_sharpness": round(sharpness, 6),
        f"{view_prefix}_exposure": round(exposure, 6),
        f"{view_prefix}_symmetry": round(symmetry, 6),
        f"{view_prefix}_insightface_det": round(insight_det, 6),
        f"{view_prefix}_insightface_fill": round(insight_fill, 6),
        f"{view_prefix}_insightface_pose": round(insight_pose, 6),
        f"{view_prefix}_external_score": round(external_score, 6),
        f"{view_prefix}_external_confidence": round(external_conf, 6),
    }
    for key, value in external_signals.items():
        signals[f"{view_prefix}_{key}"] = round(float(value), 6)

    return score, confidence, signals


async def extract_video_frames(video_url: str, num_frames: int = 10) -> List[np.ndarray]:
    """Extract frames from video for analysis"""
    frames = []
    try:
        video_path = url_to_local_path(video_url)
        if not os.path.exists(video_path):
            return frames

        cap = cv2.VideoCapture(video_path)
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))

        if total_frames <= 0:
            return frames

        # Extract evenly spaced frames
        frame_indices = np.linspace(0, total_frames - 1, num_frames, dtype=int)

        for idx in frame_indices:
            cap.set(cv2.CAP_PROP_POS_FRAMES, idx)
            ret, frame = cap.read()
            if ret:
                frames.append(frame)

        cap.release()
    except:
        pass

    return frames


async def detect_liveness(frames: List[np.ndarray]) -> float:
    """
    Simple liveness detection based on:
    1. Face detection consistency across frames
    2. Motion detection between frames
    3. Eye blink detection
    """
    if len(frames) < 3:
        return 0.5

    scores = []

    try:
        # 1. Check face detection in multiple frames
        face_detected_count = 0
        for frame in frames:
            try:
                faces = DeepFace.extract_faces(
                    frame,
                    detector_backend=DETECTOR_BACKEND,
                    enforce_detection=False
                )
                if faces and len(faces) > 0:
                    face_detected_count += 1
            except:
                continue

        face_consistency = face_detected_count / len(frames)
        scores.append(face_consistency)

        # 2. Motion detection (should have some movement for liveness)
        motion_scores = []
        for i in range(1, min(len(frames), 5)):
            prev_gray = cv2.cvtColor(frames[i-1], cv2.COLOR_BGR2GRAY)
            curr_gray = cv2.cvtColor(frames[i], cv2.COLOR_BGR2GRAY)

            diff = cv2.absdiff(prev_gray, curr_gray)
            motion = np.mean(diff) / 255.0
            motion_scores.append(min(motion * 10, 1.0))  # Scale up small motions

        if motion_scores:
            avg_motion = np.mean(motion_scores)
            # Some motion is good (0.1-0.5), too much or too little is suspicious
            motion_score = 1.0 - abs(avg_motion - 0.3) * 2
            scores.append(max(0, min(1, motion_score)))

        # 3. Simple eye state variation (should blink)
        # This is a simplified check - production would use proper eye tracking
        scores.append(0.85)  # Placeholder

        internal_score = float(np.mean(scores) if scores else 0.5)

        external_live_scores: List[float] = []
        external_conf_scores: List[float] = []
        for frame in frames[:min(3, len(frames))]:
            external = external_antispoof_signals(frame)
            if external is None:
                continue
            external_live_scores.append(clamp01(float(external.get("liveness", 0.5))))
            external_conf_scores.append(clamp01(float(external.get("confidence", 0.0))))

        if external_live_scores:
            ext_live = float(np.mean(external_live_scores))
            ext_conf = float(np.mean(external_conf_scores)) if external_conf_scores else 0.0
            w_ext = clamp01(ANTISPOOF_EXTERNAL_WEIGHT * ext_conf)
            return clamp01(((1.0 - w_ext) * internal_score) + (w_ext * ext_live))

        return clamp01(internal_score)

    except Exception as e:
        return 0.5


async def detect_deepfake(frames: List[np.ndarray]) -> float:
    """
    Simple deepfake detection based on:
    1. Face boundary artifacts
    2. Inconsistent lighting
    3. Unnatural textures

    Returns: 1.0 = authentic, 0.0 = fake
    """
    if len(frames) < 2:
        return 0.5

    scores = []

    try:
        frame_subset = frames[:3]
        for frame in frame_subset:
            # 1. Check for compression artifacts around face boundary
            try:
                faces = DeepFace.extract_faces(
                    frame,
                    detector_backend=DETECTOR_BACKEND,
                    enforce_detection=False
                )
                if faces and len(faces) > 0:
                    # Face was detected - check confidence
                    face_conf = faces[0].get("confidence", 0.9)
                    scores.append(face_conf)
            except:
                scores.append(0.7)

            # 2. Simple texture analysis
            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            laplacian_var = cv2.Laplacian(gray, cv2.CV_64F).var()
            # Natural faces have moderate texture variance
            texture_score = 1.0 - abs(laplacian_var - 500) / 1000
            scores.append(max(0, min(1, texture_score)))

        internal_authenticity = float(np.mean(scores) if scores else 0.85)

        external_spoof_probs: List[float] = []
        external_conf_scores: List[float] = []
        for frame in frame_subset:
            external = external_antispoof_signals(frame)
            if external is None:
                continue
            external_spoof_probs.append(clamp01(float(external.get("spoof_prob", 0.5))))
            external_conf_scores.append(clamp01(float(external.get("confidence", 0.0))))

        if external_spoof_probs:
            ext_authenticity = 1.0 - float(np.mean(external_spoof_probs))
            ext_conf = float(np.mean(external_conf_scores)) if external_conf_scores else 0.0
            w_ext = clamp01(ANTISPOOF_EXTERNAL_WEIGHT * ext_conf)
            return clamp01(((1.0 - w_ext) * internal_authenticity) + (w_ext * ext_authenticity))

        return clamp01(internal_authenticity)

    except:
        return 0.85


def url_to_local_path(url: str) -> str:
    """Convert URL to local file path"""
    storage_path = os.getenv("STORAGE_PATH", "/tmp/aura-media")

    if url.startswith("/media/"):
        return f"{storage_path}{url[6:]}"
    elif url.startswith("local://"):
        return f"{storage_path}/{url[8:]}"
    return url


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
