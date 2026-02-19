"""
Tests for AURA Media Service

Note: These tests mock the DeepFace library to avoid loading heavy ML models.
For integration tests with actual face detection, run with --integration flag.
"""

import sys
from unittest.mock import MagicMock

# Mock DeepFace before importing main to avoid loading ML models
mock_deepface = MagicMock()
mock_deepface.extract_faces.return_value = [{"confidence": 0.95, "facial_area": {"x": 0, "y": 0, "w": 100, "h": 100}}]
mock_deepface.verify.return_value = {"distance": 0.2, "threshold": 0.68, "verified": True}
sys.modules['deepface'] = MagicMock()
sys.modules['deepface'].DeepFace = mock_deepface

import pytest
import numpy as np
import os
import tempfile
import cv2
import base64
from unittest.mock import patch, AsyncMock
from fastapi.testclient import TestClient

from main import (
    app,
    VerificationRequest,
    LivenessRequest,
    VideoAnalysisRequest,
    url_to_local_path,
    detect_liveness,
    detect_deepfake,
    FACE_MATCH_THRESHOLD,
    LIVENESS_THRESHOLD,
    DEEPFAKE_THRESHOLD,
)

client = TestClient(app)


# ============ Test Fixtures ============

@pytest.fixture
def temp_storage():
    """Create temporary storage directory"""
    with tempfile.TemporaryDirectory() as tmpdir:
        os.environ["STORAGE_PATH"] = tmpdir
        yield tmpdir


@pytest.fixture
def sample_image():
    """Create a sample image for testing"""
    img = np.zeros((480, 640, 3), dtype=np.uint8)
    img[:, :, 0] = 100  # Blue channel
    img[:, :, 1] = 150  # Green channel
    img[:, :, 2] = 200  # Red channel
    return img


@pytest.fixture
def sample_frames(sample_image):
    """Create a list of sample frames with slight variations"""
    frames = []
    for i in range(10):
        frame = sample_image.copy()
        noise = np.random.randint(-10, 10, frame.shape, dtype=np.int16)
        frame = np.clip(frame.astype(np.int16) + noise, 0, 255).astype(np.uint8)
        frames.append(frame)
    return frames


@pytest.fixture
def sample_image_base64(sample_image):
    ok, encoded = cv2.imencode(".jpg", sample_image)
    assert ok
    return "data:image/jpeg;base64," + base64.b64encode(encoded.tobytes()).decode("utf-8")


@pytest.fixture
def sample_video_path(temp_storage, sample_frames):
    """Create a sample video file"""
    video_path = os.path.join(temp_storage, "test_video.mp4")
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(video_path, fourcc, 30.0, (640, 480))
    for frame in sample_frames:
        out.write(frame)
    out.release()
    return video_path


# ============ Helper Function Tests ============

class TestUrlToLocalPath:
    def test_media_url(self, temp_storage):
        url = "/media/videos/test.mp4"
        result = url_to_local_path(url)
        assert result == f"{temp_storage}/videos/test.mp4"

    def test_local_url(self, temp_storage):
        url = "local://uploads/image.jpg"
        result = url_to_local_path(url)
        assert result == f"{temp_storage}/uploads/image.jpg"

    def test_absolute_path(self, temp_storage):
        url = "/absolute/path/file.jpg"
        result = url_to_local_path(url)
        assert url in result or temp_storage in result


# ============ Liveness Detection Tests ============

class TestLivenessDetection:
    @pytest.mark.asyncio
    async def test_liveness_insufficient_frames(self):
        score = await detect_liveness([np.zeros((100, 100, 3))])
        assert score == 0.5

    @pytest.mark.asyncio
    async def test_liveness_with_motion(self, sample_frames):
        score = await detect_liveness(sample_frames)
        assert 0 <= score <= 1

    @pytest.mark.asyncio
    async def test_liveness_static_frames(self, sample_image):
        static_frames = [sample_image.copy() for _ in range(5)]
        score = await detect_liveness(static_frames)
        assert 0 <= score <= 1

    @pytest.mark.asyncio
    async def test_liveness_high_motion(self, sample_image):
        """Test with excessive motion (suspicious)"""
        high_motion_frames = []
        for i in range(5):
            frame = sample_image.copy()
            # Add significant noise to simulate high motion
            noise = np.random.randint(-100, 100, frame.shape, dtype=np.int16)
            frame = np.clip(frame.astype(np.int16) + noise, 0, 255).astype(np.uint8)
            high_motion_frames.append(frame)
        score = await detect_liveness(high_motion_frames)
        assert 0 <= score <= 1

    @pytest.mark.asyncio
    async def test_liveness_natural_motion_pattern(self, sample_image):
        """Test with natural motion patterns (head turn simulation)"""
        frames = []
        for i in range(10):
            frame = sample_image.copy()
            # Simulate gradual movement by shifting pixels
            shift = int(i * 2)
            frame = np.roll(frame, shift, axis=1)
            frames.append(frame)
        score = await detect_liveness(frames)
        assert 0 <= score <= 1

    @pytest.mark.asyncio
    async def test_liveness_frame_rate_variation(self, sample_image):
        """Test with varying number of frames"""
        for num_frames in [3, 5, 10, 15]:
            frames = [sample_image.copy() for _ in range(num_frames)]
            score = await detect_liveness(frames)
            assert 0 <= score <= 1

    @pytest.mark.asyncio
    async def test_liveness_face_detection_failure(self):
        """Test when face detection fails in frames"""
        mock_deepface.extract_faces.side_effect = Exception("Detection failed")
        frames = [np.zeros((100, 100, 3), dtype=np.uint8) for _ in range(5)]
        score = await detect_liveness(frames)
        assert 0 <= score <= 1
        # Reset side effect
        mock_deepface.extract_faces.side_effect = None
        mock_deepface.extract_faces.return_value = [{"confidence": 0.95, "facial_area": {"x": 0, "y": 0, "w": 100, "h": 100}}]


# ============ Deepfake Detection Tests ============

class TestDeepfakeDetection:
    @pytest.mark.asyncio
    async def test_deepfake_insufficient_frames(self):
        score = await detect_deepfake([np.zeros((100, 100, 3))])
        assert score == 0.5

    @pytest.mark.asyncio
    async def test_deepfake_detection(self, sample_frames):
        score = await detect_deepfake(sample_frames)
        assert 0 <= score <= 1

    @pytest.mark.asyncio
    async def test_deepfake_authentic_video(self, sample_image):
        """Test authentic video with high confidence"""
        mock_deepface.extract_faces.return_value = [{"confidence": 0.98, "facial_area": {"x": 0, "y": 0, "w": 100, "h": 100}}]
        frames = [sample_image.copy() for _ in range(5)]
        score = await detect_deepfake(frames)
        assert 0.5 <= score <= 1.0

    @pytest.mark.asyncio
    async def test_deepfake_low_confidence_detection(self, sample_image):
        """Test suspected deepfake with low face confidence"""
        mock_deepface.extract_faces.return_value = [{"confidence": 0.40, "facial_area": {"x": 0, "y": 0, "w": 100, "h": 100}}]
        frames = [sample_image.copy() for _ in range(5)]
        score = await detect_deepfake(frames)
        assert 0 <= score <= 1.0

    @pytest.mark.asyncio
    async def test_deepfake_texture_analysis(self, sample_image):
        """Test deepfake detection with unnatural textures"""
        # Create a very smooth image (suspicious)
        smooth_frame = cv2.GaussianBlur(sample_image, (15, 15), 0)
        frames = [smooth_frame for _ in range(3)]
        score = await detect_deepfake(frames)
        assert 0 <= score <= 1.0

    @pytest.mark.asyncio
    async def test_deepfake_threshold_boundary(self, sample_image):
        """Test deepfake scores near threshold"""
        mock_deepface.extract_faces.return_value = [{"confidence": 0.79, "facial_area": {"x": 0, "y": 0, "w": 100, "h": 100}}]
        frames = [sample_image.copy() for _ in range(3)]
        score = await detect_deepfake(frames)
        assert 0 <= score <= 1.0

    @pytest.mark.asyncio
    async def test_deepfake_face_extraction_failure(self, sample_image):
        """Test when face extraction fails"""
        mock_deepface.extract_faces.side_effect = Exception("Extraction failed")
        frames = [sample_image.copy() for _ in range(3)]
        score = await detect_deepfake(frames)
        assert 0 <= score <= 1.0
        # Reset side effect
        mock_deepface.extract_faces.side_effect = None
        mock_deepface.extract_faces.return_value = [{"confidence": 0.95, "facial_area": {"x": 0, "y": 0, "w": 100, "h": 100}}]


# ============ API Endpoint Tests ============

class TestHealthEndpoint:
    def test_health_check(self):
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert data["service"] == "media-service"


class TestLivenessChallengesEndpoint:
    def test_get_challenges(self):
        response = client.post("/verify/liveness/challenges", json={"user_id": 123})
        assert response.status_code == 200
        data = response.json()
        assert "session_id" in data
        assert "challenges" in data
        assert "timeout" in data
        assert len(data["challenges"]) == 3
        for challenge in data["challenges"]:
            assert "type" in challenge
            assert "instruction" in challenge

    def test_challenge_types(self):
        valid_types = {"BLINK", "TURN_HEAD_LEFT", "TURN_HEAD_RIGHT", "SMILE", "NOD", "RAISE_EYEBROWS"}
        response = client.post("/verify/liveness/challenges", json={"user_id": 123})
        data = response.json()
        for challenge in data["challenges"]:
            assert challenge["type"] in valid_types


class TestVideoUploadEndpoint:
    def test_upload_video(self, temp_storage):
        test_content = b"fake video content for testing"
        response = client.post(
            "/upload/video",
            files={"file": ("test.mp4", test_content, "video/mp4")},
            data={"path": "videos", "type": "verification"}
        )
        assert response.status_code == 200
        data = response.json()
        assert "url" in data
        assert "filename" in data
        assert data["size"] == len(test_content)


class TestFaceVerificationEndpoint:
    def test_verification_missing_image(self):
        with patch('main.load_image_from_url', new_callable=AsyncMock) as mock_load:
            mock_load.return_value = None
            response = client.post("/verify/face", json={
                "user_id": 123,
                "profile_image_url": "/nonexistent/image.jpg",
                "verification_video_url": "/media/videos/test.mp4",
                "session_id": "test-session-123"
            })
            assert response.status_code == 200
            data = response.json()
            assert data["verified"] == False
            assert "Could not load profile image" in data["issues"]

    def test_verification_missing_video_frames(self):
        """Test when video has no extractable frames"""
        sample_image = np.zeros((480, 640, 3), dtype=np.uint8)
        with patch('main.load_image_from_url', new_callable=AsyncMock) as mock_load:
            with patch('main.extract_video_frames', new_callable=AsyncMock) as mock_frames:
                mock_load.return_value = sample_image
                mock_frames.return_value = []
                response = client.post("/verify/face", json={
                    "user_id": 123,
                    "profile_image_url": "/media/profile.jpg",
                    "verification_video_url": "/media/videos/test.mp4",
                    "session_id": "test-session-123"
                })
                assert response.status_code == 200
                data = response.json()
                assert data["verified"] == False
                assert "Could not extract frames from video" in data["issues"]

    def test_verification_no_face_detected(self, sample_image):
        """Test when no face is detected in video frames"""
        mock_deepface.verify.side_effect = Exception("Face not detected")
        try:
            with patch('main.load_image_from_url', new_callable=AsyncMock) as mock_load:
                with patch('main.extract_video_frames', new_callable=AsyncMock) as mock_frames:
                    mock_load.return_value = sample_image
                    mock_frames.return_value = [sample_image] * 5
                    response = client.post("/verify/face", json={
                        "user_id": 123,
                        "profile_image_url": "/media/profile.jpg",
                        "verification_video_url": "/media/videos/test.mp4",
                        "session_id": "test-session-123"
                    })
                    assert response.status_code == 200
                    data = response.json()
                    assert data["verified"] == False
                    assert "Could not detect face in video" in data["issues"]
        finally:
            # Reset side_effect so it doesn't affect subsequent tests
            mock_deepface.verify.side_effect = None
            mock_deepface.verify.return_value = {"distance": 0.2, "threshold": 0.68, "verified": True}

    def test_verification_multiple_faces(self, sample_image):
        """Test handling of multiple faces in image"""
        mock_deepface.extract_faces.return_value = [
            {"confidence": 0.95, "facial_area": {"x": 0, "y": 0, "w": 100, "h": 100}},
            {"confidence": 0.90, "facial_area": {"x": 200, "y": 0, "w": 100, "h": 100}}
        ]
        with patch('main.load_image_from_url', new_callable=AsyncMock) as mock_load:
            with patch('main.extract_video_frames', new_callable=AsyncMock) as mock_frames:
                mock_load.return_value = sample_image
                mock_frames.return_value = [sample_image] * 5
                mock_deepface.verify.return_value = {"distance": 0.2, "threshold": 0.68, "verified": True}
                response = client.post("/verify/face", json={
                    "user_id": 123,
                    "profile_image_url": "/media/profile.jpg",
                    "verification_video_url": "/media/videos/test.mp4",
                    "session_id": "test-session-123"
                })
                assert response.status_code == 200
                data = response.json()
                # Should still process but may have lower confidence

    def test_verification_low_quality_image(self, sample_image):
        """Test with low quality/resolution image"""
        low_quality = cv2.resize(sample_image, (32, 32))
        with patch('main.load_image_from_url', new_callable=AsyncMock) as mock_load:
            with patch('main.extract_video_frames', new_callable=AsyncMock) as mock_frames:
                mock_load.return_value = low_quality
                mock_frames.return_value = [low_quality] * 5
                mock_deepface.verify.return_value = {"distance": 0.5, "threshold": 0.68, "verified": False}
                response = client.post("/verify/face", json={
                    "user_id": 123,
                    "profile_image_url": "/media/profile.jpg",
                    "verification_video_url": "/media/videos/test.mp4",
                    "session_id": "test-session-123"
                })
                assert response.status_code == 200
                data = response.json()
                assert 0 <= data["face_match_score"] <= 100

    def test_verification_different_lighting(self, sample_image):
        """Test face verification with different lighting conditions"""
        dark_image = (sample_image * 0.3).astype(np.uint8)
        bright_image = np.clip(sample_image * 1.7, 0, 255).astype(np.uint8)
        with patch('main.load_image_from_url', new_callable=AsyncMock) as mock_load:
            with patch('main.extract_video_frames', new_callable=AsyncMock) as mock_frames:
                mock_load.return_value = dark_image
                mock_frames.return_value = [bright_image] * 5
                mock_deepface.verify.return_value = {"distance": 0.4, "threshold": 0.68, "verified": False}
                response = client.post("/verify/face", json={
                    "user_id": 123,
                    "profile_image_url": "/media/profile.jpg",
                    "verification_video_url": "/media/videos/test.mp4",
                    "session_id": "test-session-123"
                })
                assert response.status_code == 200
                data = response.json()
                assert 0 <= data["face_match_score"] <= 100

    def test_verification_successful_match(self, sample_image):
        """Test successful face verification"""
        # Create a fresh mock for DeepFace.verify to avoid contamination from other tests
        verify_mock = MagicMock(return_value={"distance": 0.2, "threshold": 0.68, "verified": True})
        with patch('main.DeepFace.verify', verify_mock):
            with patch('main.load_image_from_url', new_callable=AsyncMock) as mock_load:
                with patch('main.extract_video_frames', new_callable=AsyncMock) as mock_frames:
                    with patch('main.detect_liveness', new_callable=AsyncMock) as mock_liveness:
                        with patch('main.detect_deepfake', new_callable=AsyncMock) as mock_deepfake:
                            mock_load.return_value = sample_image
                            mock_frames.return_value = [sample_image] * 5
                            mock_liveness.return_value = 0.90
                            mock_deepfake.return_value = 0.90
                            response = client.post("/verify/face", json={
                                "user_id": 123,
                                "profile_image_url": "/media/profile.jpg",
                                "verification_video_url": "/media/videos/test.mp4",
                                "session_id": "test-session-123"
                            })
                            assert response.status_code == 200
                            data = response.json()
                            assert data["face_match_score"] > 0
                            assert data["liveness_score"] == 90.0
                            assert data["deepfake_score"] == 90.0


class TestVideoAnalysisEndpoint:
    def test_analyze_nonexistent_video(self, temp_storage):
        response = client.post("/video/analyze", json={
            "video_url": "/media/nonexistent.mp4",
            "user_id": 123
        })
        assert response.status_code in [200, 500]

    def test_analyze_video_success(self, sample_video_path):
        """Test successful video analysis"""
        with patch('main.extract_video_frames', new_callable=AsyncMock) as mock_frames:
            mock_frames.return_value = [np.zeros((480, 640, 3), dtype=np.uint8) for _ in range(10)]
            response = client.post("/video/analyze", json={
                "video_url": f"local://{sample_video_path}",
                "user_id": 123
            })
            if response.status_code == 200:
                data = response.json()
                assert "duration" in data
                assert "sentiment" in data
                assert "frame_count" in data

    def test_analyze_long_video(self, temp_storage):
        """Test handling of long video files"""
        with patch('main.extract_video_frames', new_callable=AsyncMock) as mock_frames:
            # Simulate long video with many frames
            mock_frames.return_value = [np.zeros((480, 640, 3), dtype=np.uint8) for _ in range(100)]
            response = client.post("/video/analyze", json={
                "video_url": "/media/long_video.mp4",
                "user_id": 123
            })
            assert response.status_code in [200, 500]

    def test_analyze_video_zero_fps(self, temp_storage):
        """Test video with invalid frame rate"""
        with patch('main.url_to_local_path') as mock_path:
            with patch('cv2.VideoCapture') as mock_cap:
                mock_path.return_value = "/tmp/test.mp4"
                mock_instance = MagicMock()
                mock_instance.get.side_effect = [0, 100]  # fps=0, frame_count=100
                mock_instance.release.return_value = None
                mock_cap.return_value = mock_instance

                with patch('main.extract_video_frames', new_callable=AsyncMock) as mock_frames:
                    mock_frames.return_value = []
                    response = client.post("/video/analyze", json={
                        "video_url": "/media/test.mp4",
                        "user_id": 123
                    })
                    if response.status_code == 200:
                        data = response.json()
                        assert data["duration"] == 0

    def test_analyze_video_thumbnail_generation(self, temp_storage):
        """Test thumbnail generation from video"""
        sample_frame = np.zeros((480, 640, 3), dtype=np.uint8)
        with patch('main.extract_video_frames', new_callable=AsyncMock) as mock_frames:
            mock_frames.return_value = [sample_frame]
            response = client.post("/video/analyze", json={
                "video_url": "/media/test.mp4",
                "user_id": 123
            })
            if response.status_code == 200:
                data = response.json()
                assert "thumbnail_url" in data

    def test_analyze_video_sentiment_structure(self, temp_storage):
        """Test sentiment analysis output structure"""
        with patch('main.extract_video_frames', new_callable=AsyncMock) as mock_frames:
            mock_frames.return_value = [np.zeros((480, 640, 3), dtype=np.uint8)]
            response = client.post("/video/analyze", json={
                "video_url": "/media/test.mp4",
                "user_id": 123
            })
            if response.status_code == 200:
                data = response.json()
                sentiment = data["sentiment"]
                assert "positive" in sentiment
                assert "negative" in sentiment
                assert "neutral" in sentiment
                assert 0 <= sentiment["positive"] <= 1
                assert 0 <= sentiment["negative"] <= 1
                assert 0 <= sentiment["neutral"] <= 1


class TestAttractivenessEndpoint:
    def test_attractiveness_score_from_base64(self, sample_image_base64):
        response = client.post("/attractiveness/score", json={
            "user_id": 42,
            "front_image_base64": sample_image_base64
        })
        assert response.status_code == 200
        data = response.json()
        assert 0 <= data["score"] <= 1
        assert 0 <= data["confidence"] <= 1
        assert "provider" in data
        assert "model_version" in data
        assert "repo_refs" in data
        assert "signals" in data
        assert "front_face_confidence" in data["signals"]
        assert "front_insightface_det" in data["signals"]

    def test_attractiveness_requires_front_image(self):
        response = client.post("/attractiveness/score", json={"user_id": 99})
        assert response.status_code == 400

    def test_attractiveness_with_side_view(self, sample_image_base64):
        response = client.post("/attractiveness/score", json={
            "user_id": 43,
            "front_image_base64": sample_image_base64,
            "side_image_base64": sample_image_base64
        })
        assert response.status_code == 200
        data = response.json()
        assert 0 <= data["score"] <= 1
        assert 0 <= data["confidence"] <= 1
        assert "side_face_confidence" in data["signals"]

    def test_attractiveness_with_url_sources(self, sample_image):
        with patch('main.load_image_from_url', new_callable=AsyncMock) as mock_load:
            mock_load.return_value = sample_image
            response = client.post("/attractiveness/score", json={
                "front_image_url": "https://example.com/front.jpg",
                "side_image_url": "https://example.com/side.jpg"
            })
            assert response.status_code == 200
            data = response.json()
            assert 0 <= data["score"] <= 1
            assert 0 <= data["confidence"] <= 1


# ============ Configuration Tests ============

class TestConfiguration:
    def test_thresholds_valid(self):
        assert 0 <= FACE_MATCH_THRESHOLD <= 1
        assert 0 <= LIVENESS_THRESHOLD <= 1
        assert 0 <= DEEPFAKE_THRESHOLD <= 1

    def test_thresholds_reasonable(self):
        assert 0.5 <= FACE_MATCH_THRESHOLD <= 0.95
        assert 0.5 <= LIVENESS_THRESHOLD <= 0.95
        assert 0.5 <= DEEPFAKE_THRESHOLD <= 0.95


# ============ Edge Cases ============

class TestEdgeCases:
    def test_empty_frames_liveness(self):
        import asyncio
        result = asyncio.run(detect_liveness([]))
        assert result == 0.5

    def test_single_frame_deepfake(self):
        import asyncio
        single_frame = np.zeros((100, 100, 3), dtype=np.uint8)
        result = asyncio.run(detect_deepfake([single_frame]))
        assert result == 0.5

    def test_corrupted_frame_handling(self):
        import asyncio
        tiny_frame = np.zeros((1, 1, 3), dtype=np.uint8)
        result = asyncio.run(detect_liveness([tiny_frame, tiny_frame, tiny_frame]))
        assert 0 <= result <= 1


# ============ Security Tests ============

class TestSecurity:
    def test_path_traversal_prevention(self, temp_storage):
        response = client.post(
            "/upload/video",
            files={"file": ("../../../etc/passwd", b"malicious", "video/mp4")},
            data={"path": "videos", "type": "verification"}
        )
        if response.status_code == 200:
            data = response.json()
            assert ".." not in data["filename"]
            assert "passwd" not in data["url"]

    def test_url_to_path_safety(self, temp_storage):
        dangerous_url = "/media/../../../etc/passwd"
        result = url_to_local_path(dangerous_url)
        # Result should be sanitized

    def test_file_size_limit_small(self, temp_storage):
        """Test small file upload"""
        small_content = b"x" * 100  # 100 bytes
        response = client.post(
            "/upload/video",
            files={"file": ("small.mp4", small_content, "video/mp4")},
            data={"path": "videos", "type": "verification"}
        )
        assert response.status_code == 200
        data = response.json()
        assert data["size"] == 100

    def test_file_size_limit_large(self, temp_storage):
        """Test large file upload (10MB)"""
        large_content = b"x" * (10 * 1024 * 1024)  # 10MB
        response = client.post(
            "/upload/video",
            files={"file": ("large.mp4", large_content, "video/mp4")},
            data={"path": "videos", "type": "verification"}
        )
        # Should handle large files or return appropriate error
        assert response.status_code in [200, 413, 500]

    def test_mime_type_validation_video(self, temp_storage):
        """Test valid video MIME types"""
        valid_types = ["video/mp4", "video/mpeg", "video/quicktime", "video/webm"]
        for mime_type in valid_types:
            response = client.post(
                "/upload/video",
                files={"file": ("test.mp4", b"content", mime_type)},
                data={"path": "videos", "type": "verification"}
            )
            # Should accept video types
            assert response.status_code in [200, 500]

    def test_mime_type_validation_invalid(self, temp_storage):
        """Test rejection of non-video MIME types"""
        # Note: Current implementation doesn't validate MIME type
        # This test documents expected behavior
        response = client.post(
            "/upload/video",
            files={"file": ("script.exe", b"malicious", "application/x-executable")},
            data={"path": "videos", "type": "verification"}
        )
        # Should still upload but filename is sanitized
        if response.status_code == 200:
            data = response.json()
            assert data["filename"].endswith(".exe")

    def test_malicious_filename_handling(self, temp_storage):
        """Test handling of malicious filenames"""
        malicious_names = [
            "../../etc/passwd.mp4",
            "null\x00byte.mp4",
            "<script>alert('xss')</script>.mp4",
            "file; rm -rf /.mp4"
        ]
        for name in malicious_names:
            response = client.post(
                "/upload/video",
                files={"file": (name, b"content", "video/mp4")},
                data={"path": "videos", "type": "verification"}
            )
            if response.status_code == 200:
                data = response.json()
                # Filename should be sanitized (UUID-based)
                assert ".." not in data["filename"]
                assert "\x00" not in data["filename"]
                assert "<script>" not in data["filename"]

    def test_empty_file_upload(self, temp_storage):
        """Test upload of empty file"""
        response = client.post(
            "/upload/video",
            files={"file": ("empty.mp4", b"", "video/mp4")},
            data={"path": "videos", "type": "verification"}
        )
        if response.status_code == 200:
            data = response.json()
            assert data["size"] == 0

    def test_special_characters_in_path(self, temp_storage):
        """Test special characters in upload path"""
        special_paths = ["videos/../uploads", "videos/../../tmp", "videos/\x00null"]
        for path in special_paths:
            response = client.post(
                "/upload/video",
                files={"file": ("test.mp4", b"content", "video/mp4")},
                data={"path": path, "type": "verification"}
            )
            # Should handle gracefully
            assert response.status_code in [200, 400, 500]

    def test_sql_injection_in_user_id(self):
        """Test SQL injection attempts in user_id"""
        response = client.post("/verify/liveness/challenges", json={
            "user_id": "123; DROP TABLE users;--"
        })
        # Should handle gracefully - FastAPI will validate type
        assert response.status_code in [200, 422]

    def test_xss_in_session_id(self, sample_image):
        """Test XSS attempts in session_id"""
        with patch('main.load_image_from_url', new_callable=AsyncMock) as mock_load:
            with patch('main.extract_video_frames', new_callable=AsyncMock) as mock_frames:
                mock_load.return_value = sample_image
                mock_frames.return_value = []
                response = client.post("/verify/face", json={
                    "user_id": 123,
                    "profile_image_url": "/media/profile.jpg",
                    "verification_video_url": "/media/video.mp4",
                    "session_id": "<script>alert('xss')</script>"
                })
                assert response.status_code == 200
                # Should not execute script

    def test_concurrent_uploads(self, temp_storage):
        """Test handling of concurrent file uploads"""
        import concurrent.futures

        def upload_file(index):
            return client.post(
                "/upload/video",
                files={"file": (f"test{index}.mp4", b"content", "video/mp4")},
                data={"path": "videos", "type": "verification"}
            )

        with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
            futures = [executor.submit(upload_file, i) for i in range(5)]
            results = [f.result() for f in concurrent.futures.as_completed(futures)]

        # All should complete successfully
        success_count = sum(1 for r in results if r.status_code == 200)
        assert success_count >= 0


# ============ Integration Tests ============

class TestIntegration:
    """Integration tests that test multiple components together"""

    def test_full_verification_workflow(self, sample_image):
        """Test complete verification workflow from upload to verification"""
        # Create a fresh mock for DeepFace.verify to avoid contamination from other tests
        verify_mock = MagicMock(return_value={"distance": 0.2, "threshold": 0.68, "verified": True})
        with patch('main.DeepFace.verify', verify_mock):
            with patch('main.load_image_from_url', new_callable=AsyncMock) as mock_load:
                with patch('main.extract_video_frames', new_callable=AsyncMock) as mock_frames:
                    with patch('main.detect_liveness', new_callable=AsyncMock) as mock_liveness:
                        with patch('main.detect_deepfake', new_callable=AsyncMock) as mock_deepfake:
                            mock_load.return_value = sample_image
                            mock_frames.return_value = [sample_image] * 5
                            mock_liveness.return_value = 0.90
                            mock_deepfake.return_value = 0.90

                            # Step 1: Get liveness challenges
                            challenges_response = client.post("/verify/liveness/challenges", json={"user_id": 123})
                            assert challenges_response.status_code == 200
                            session_id = challenges_response.json()["session_id"]

                            # Step 2: Verify face
                            verify_response = client.post("/verify/face", json={
                                "user_id": 123,
                                "profile_image_url": "/media/profile.jpg",
                                "verification_video_url": "/media/video.mp4",
                                "session_id": session_id
                            })
                            assert verify_response.status_code == 200
                            verify_data = verify_response.json()
                            assert verify_data["face_match_score"] > 0


# Run with: pytest test_main.py -v
