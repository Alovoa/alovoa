#!/usr/bin/env python3
"""
Detoxify adapter for text moderation.

Output contract:
{
  "labels": {"toxicity": 0..1, ...},
  "toxicity_score": 0..1,
  "max_label": "toxicity",
  "provider": "detoxify",
  "model_version": "detoxify_multilingual",
  "signals": { ... }
}
"""

from __future__ import annotations

import argparse
import json
import re
from typing import Dict, Optional


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def round6(value: float) -> float:
    return round(float(value), 6)


def _scalar(value) -> float:
    if isinstance(value, (list, tuple)) and value:
        return float(value[0])
    try:
        import numpy as np  # type: ignore
        if hasattr(value, "shape"):
            arr = np.asarray(value).reshape(-1)
            if arr.size:
                return float(arr[0])
    except Exception:
        pass
    return float(value)


def run_detoxify(text: str, model_name: str) -> Optional[dict]:
    try:
        from detoxify import Detoxify  # type: ignore
    except Exception:
        return None

    try:
        model = Detoxify(model_name)
        raw = model.predict(text)
    except Exception:
        return None

    labels: Dict[str, float] = {}
    for key, value in (raw or {}).items():
        try:
            labels[str(key).strip().lower()] = round6(clamp01(_scalar(value)))
        except Exception:
            continue

    if not labels:
        return None

    max_label = max(labels.items(), key=lambda x: x[1])[0]
    toxicity_score = labels.get("toxicity", max(labels.values()))

    return {
        "labels": labels,
        "toxicity_score": round6(clamp01(toxicity_score)),
        "max_label": max_label,
        "provider": "detoxify",
        "model_version": f"detoxify_{model_name}",
        "signals": {
            "provider_detoxify": 1.0,
            "label_count": float(len(labels)),
        },
    }


HIGH_RISK_PATTERNS = [
    r"\bkill\b",
    r"\brape\b",
    r"\bdie\b",
    r"\bnazi\b",
    r"\bterrorist\b",
    r"\bbitch\b",
    r"\bcunt\b",
    r"\basshole\b",
    r"\bfag\b",
    r"\bnigger\b",
    r"\bkike\b",
]


def heuristic(text: str) -> dict:
    lowered = text.lower()
    hits = 0
    for pattern in HIGH_RISK_PATTERNS:
        if re.search(pattern, lowered):
            hits += 1

    toxicity = clamp01(0.05 + min(0.85, 0.18 * hits))
    labels = {
        "toxicity": round6(toxicity),
        "threat": round6(clamp01(0.55 * toxicity if "kill" in lowered else 0.05 * toxicity)),
        "insult": round6(clamp01(0.70 * toxicity)),
        "obscene": round6(clamp01(0.60 * toxicity)),
        "identity_attack": round6(clamp01(0.65 * toxicity if ("nigger" in lowered or "kike" in lowered) else 0.04 * toxicity)),
        "severe_toxicity": round6(clamp01(0.75 * toxicity)),
    }
    max_label = max(labels.items(), key=lambda x: x[1])[0]

    return {
        "labels": labels,
        "toxicity_score": round6(toxicity),
        "max_label": max_label,
        "provider": "keyword_heuristic",
        "model_version": "heuristic_v1",
        "signals": {
            "provider_fallback": 1.0,
            "risk_hits": float(hits),
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--text", required=True)
    parser.add_argument("--model", default="multilingual")
    args = parser.parse_args()

    payload = run_detoxify(args.text, args.model)
    if payload is None:
        payload = heuristic(args.text)

    print(json.dumps(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
