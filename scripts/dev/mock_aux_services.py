#!/usr/bin/env python3
"""
Lightweight no-dependency mock servers for local UX validation.
Serves:
  - media-service on :8001
  - ai-service on :8002
"""

from __future__ import annotations

import json
import signal
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any, Dict


def _read_json(handler: BaseHTTPRequestHandler) -> Dict[str, Any]:
    content_length = int(handler.headers.get("Content-Length", "0") or "0")
    if content_length <= 0:
        return {}
    body = handler.rfile.read(content_length)
    if not body:
        return {}
    try:
        return json.loads(body.decode("utf-8"))
    except Exception:
        return {}


def _write_json(handler: BaseHTTPRequestHandler, status: int, payload: Dict[str, Any]) -> None:
    raw = json.dumps(payload).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json")
    handler.send_header("Content-Length", str(len(raw)))
    handler.end_headers()
    handler.wfile.write(raw)


class _BaseHandler(BaseHTTPRequestHandler):
    server_version = "AloVoaMock/1.0"

    def log_message(self, fmt: str, *args: Any) -> None:
        # Silence request logs to keep output clean for scripts.
        return


class MediaHandler(_BaseHandler):
    def do_GET(self) -> None:  # noqa: N802 (HTTP verb signature)
        if self.path == "/health":
            _write_json(self, 200, {"status": "healthy", "service": "media-service"})
            return
        _write_json(self, 404, {"error": "not_found"})

    def do_POST(self) -> None:  # noqa: N802
        body = _read_json(self)

        if self.path in ("/moderation/image", "/api/v1/moderation/image"):
            _write_json(
                self,
                200,
                {
                    "is_safe": True,
                    "nsfw_score": 0.01,
                    "confidence": 0.95,
                    "action": "ALLOW",
                    "provider": "mock_media",
                    "model_version": "mock_v1",
                    "categories": {"adult": 0.01, "violence": 0.0, "hate": 0.0},
                    "signals": {},
                    "repo_refs": [],
                },
            )
            return

        if self.path in ("/moderation/text", "/api/v1/moderation/text"):
            text = body.get("text", "")
            _write_json(
                self,
                200,
                {
                    "is_allowed": True,
                    "decision": "allow",
                    "toxicity_score": 0.02,
                    "max_label": "toxicity",
                    "blocked_categories": [],
                    "labels": {"toxicity": 0.02},
                    "reason": None,
                    "provider": "mock_media",
                    "model_version": "mock_v1",
                    "signals": {"text_length": len(text)},
                    "repo_refs": [],
                },
            )
            return

        if self.path in ("/quality/face", "/api/v1/quality/face"):
            _write_json(
                self,
                200,
                {
                    "quality_score": 0.82,
                    "confidence": 0.88,
                    "provider": "mock_faceqan",
                    "model_version": "mock_v1",
                    "signals": {"brightness": 0.8, "sharpness": 0.83},
                    "repo_refs": [],
                },
            )
            return

        if self.path in ("/attractiveness/score", "/api/v1/attractiveness/score"):
            _write_json(
                self,
                200,
                {
                    "score": 0.5,
                    "confidence": 0.7,
                    "provider": "mock_attractiveness",
                    "model_version": "mock_v1",
                    "signals": {"baseline": 0.5},
                    "repo_refs": [],
                },
            )
            return

        if self.path in ("/api/v1/verify-face", "/api/v1/liveness-check", "/api/v1/analyze-video"):
            _write_json(
                self,
                200,
                {
                    "verified": True,
                    "face_match_score": 0.92,
                    "liveness_score": 0.95,
                    "deepfake_score": 0.03,
                    "status": "VERIFIED",
                },
            )
            return

        _write_json(self, 404, {"error": "not_found"})


class AiHandler(_BaseHandler):
    def do_GET(self) -> None:  # noqa: N802
        if self.path == "/health":
            _write_json(self, 200, {"status": "healthy", "service": "ai-service"})
            return
        _write_json(self, 404, {"error": "not_found"})

    def do_POST(self) -> None:  # noqa: N802
        body = _read_json(self)

        if self.path in ("/match/daily", "/api/v1/recommendations"):
            _write_json(self, 200, [])
            return

        if self.path in ("/match/calculate", "/api/v1/compatibility"):
            _write_json(
                self,
                200,
                {
                    "values_score": 70.0,
                    "lifestyle_score": 68.0,
                    "personality_score": 72.0,
                    "attraction_score": 64.0,
                    "circumstantial_score": 66.0,
                    "growth_score": 71.0,
                    "overall_score": 70.0,
                    "enemy_score": 12.0,
                    "top_compatibilities": ["Shared values", "Good communication"],
                    "potential_challenges": ["Pace mismatch"],
                    "explanation": "Mock compatibility response",
                },
            )
            return

        if self.path in ("/embeddings/personality", "/api/v1/embedding"):
            user_id = body.get("user_id", "unknown")
            _write_json(
                self,
                200,
                {
                    "embedding_id": f"mock_embedding_{user_id}",
                    "dimensions": 128,
                },
            )
            return

        _write_json(self, 404, {"error": "not_found"})


def _run_server(port: int, handler_cls: type[BaseHTTPRequestHandler]) -> ThreadingHTTPServer:
    server = ThreadingHTTPServer(("0.0.0.0", port), handler_cls)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    return server


def main() -> int:
    media_server = _run_server(8001, MediaHandler)
    ai_server = _run_server(8002, AiHandler)

    print("Mock media-service listening on :8001")
    print("Mock ai-service listening on :8002")

    stop_event = threading.Event()

    def _shutdown(*_: Any) -> None:
        stop_event.set()

    signal.signal(signal.SIGINT, _shutdown)
    signal.signal(signal.SIGTERM, _shutdown)

    try:
        stop_event.wait()
    finally:
        media_server.shutdown()
        ai_server.shutdown()
        media_server.server_close()
        ai_server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
