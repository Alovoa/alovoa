#!/usr/bin/env python3
"""
Evaluate reranker guardrails and optionally apply staged rollout updates.

This script reads recent event metrics grouped by A/B variant and computes
guardrail deltas for treatment vs control. It can then:
  - HOLD: keep current trafficPercent
  - ADVANCE: move to the next rollout stage
  - ROLLBACK: move to previous stage (or disable)

Expected schema:
  - feature_flags(flag_name, segment_key, enabled, json_config)
  - impression_events(variant, candidate_desirability_decile, ...)
  - like_events(...)
  - match_events(...)
  - message_events(...)
  - user_report(user_from_id, date)
  - user_block(user_from_id, date)
"""

from __future__ import annotations

import argparse
import json
from dataclasses import dataclass, asdict
from datetime import datetime, timedelta, timezone
from typing import Dict, Iterable, List, Tuple

from sqlalchemy import create_engine, text


def clamp(value: float, lo: float, hi: float) -> float:
    return max(lo, min(hi, float(value)))


def safe_rate(num: float, denom: float) -> float:
    if denom <= 0:
        return 0.0
    return float(num) / float(denom)


def relative_change(treatment: float, control: float) -> float:
    if abs(control) <= 1e-12:
        if abs(treatment) <= 1e-12:
            return 0.0
        return float("inf")
    return (float(treatment) - float(control)) / float(control)


def parse_stages(raw: str) -> List[int]:
    out = set()
    for token in (raw or "").split(","):
        token = token.strip()
        if not token:
            continue
        try:
            out.add(int(clamp(int(token), 0, 100)))
        except Exception:
            continue
    out.add(0)
    return sorted(out)


def parse_json_config(raw: str | None) -> Dict[str, object]:
    if not raw:
        return {}
    try:
        payload = json.loads(raw)
        if isinstance(payload, dict):
            return payload
        return {}
    except Exception:
        return {}


def pick_stage_for_percent(stages: List[int], percent: int) -> int:
    percent = int(clamp(percent, 0, 100))
    candidate = 0
    for stage in stages:
        if stage <= percent:
            candidate = stage
        else:
            break
    return candidate


def next_stage(stages: List[int], current: int) -> int:
    for stage in stages:
        if stage > current:
            return stage
    return current


def prev_stage(stages: List[int], current: int) -> int:
    previous = 0
    for stage in stages:
        if stage >= current:
            break
        previous = stage
    return previous


@dataclass
class VariantMetrics:
    impressions: int = 0
    likes: int = 0
    match_participations: int = 0
    conversations: int = 0
    reports: int = 0
    blocks: int = 0
    top_decile_impressions: int = 0

    @property
    def likes_per_impression(self) -> float:
        return safe_rate(self.likes, self.impressions)

    @property
    def matches_per_impression(self) -> float:
        return safe_rate(self.match_participations, self.impressions)

    @property
    def conversations_per_impression(self) -> float:
        return safe_rate(self.conversations, self.impressions)

    @property
    def report_rate_per_impression(self) -> float:
        return safe_rate(self.reports, self.impressions)

    @property
    def block_rate_per_impression(self) -> float:
        return safe_rate(self.blocks, self.impressions)

    @property
    def top_decile_share(self) -> float:
        return safe_rate(self.top_decile_impressions, self.impressions)

    def to_dict(self) -> Dict[str, float]:
        base = asdict(self)
        base.update({
            "likes_per_impression": self.likes_per_impression,
            "matches_per_impression": self.matches_per_impression,
            "conversations_per_impression": self.conversations_per_impression,
            "report_rate_per_impression": self.report_rate_per_impression,
            "block_rate_per_impression": self.block_rate_per_impression,
            "top_decile_share": self.top_decile_share,
        })
        return base


def latest_variant_subquery() -> str:
    return """
        SELECT
            viewer_id,
            SUBSTRING_INDEX(GROUP_CONCAT(variant ORDER BY ts DESC), ',', 1) AS variant
        FROM impression_events
        WHERE ts BETWEEN :from_ts AND :to_ts
          AND (:segment_key = '*' OR segment_key = :segment_key)
        GROUP BY viewer_id
    """


def fetch_metric_counts(
    conn,
    from_ts: datetime,
    to_ts: datetime,
    segment_key: str,
) -> Dict[str, VariantMetrics]:
    metrics: Dict[str, VariantMetrics] = {}

    def ensure(variant: str) -> VariantMetrics:
        key = (variant or "control").strip() or "control"
        if key not in metrics:
            metrics[key] = VariantMetrics()
        return metrics[key]

    params = {
        "from_ts": from_ts,
        "to_ts": to_ts,
        "segment_key": segment_key,
    }

    impression_sql = text("""
        SELECT
            variant,
            COUNT(*) AS impressions,
            SUM(CASE WHEN COALESCE(candidate_desirability_decile, 0) >= 9 THEN 1 ELSE 0 END) AS top_decile_impressions
        FROM impression_events
        WHERE ts BETWEEN :from_ts AND :to_ts
          AND (:segment_key = '*' OR segment_key = :segment_key)
        GROUP BY variant
    """)
    for row in conn.execute(impression_sql, params):
        bucket = ensure(str(row.variant))
        bucket.impressions = int(row.impressions or 0)
        bucket.top_decile_impressions = int(row.top_decile_impressions or 0)

    like_sql = text(f"""
        SELECT lv.variant AS variant, COUNT(*) AS likes
        FROM like_events le
        JOIN ({latest_variant_subquery()}) lv
          ON lv.viewer_id = le.viewer_id
        WHERE le.ts BETWEEN :from_ts AND :to_ts
          AND (:segment_key = '*' OR le.segment_key = :segment_key)
        GROUP BY lv.variant
    """)
    for row in conn.execute(like_sql, params):
        ensure(str(row.variant)).likes = int(row.likes or 0)

    match_sql = text(f"""
        SELECT lv.variant AS variant, COUNT(*) AS match_participations
        FROM (
            SELECT user_a AS user_id, segment_key, ts FROM match_events
            UNION ALL
            SELECT user_b AS user_id, segment_key, ts FROM match_events
        ) mp
        JOIN ({latest_variant_subquery()}) lv
          ON lv.viewer_id = mp.user_id
        WHERE mp.ts BETWEEN :from_ts AND :to_ts
          AND (:segment_key = '*' OR mp.segment_key = :segment_key)
        GROUP BY lv.variant
    """)
    for row in conn.execute(match_sql, params):
        ensure(str(row.variant)).match_participations = int(row.match_participations or 0)

    conversation_sql = text(f"""
        SELECT lv.variant AS variant, COUNT(DISTINCT me.conversation_id) AS conversations
        FROM message_events me
        JOIN ({latest_variant_subquery()}) lv
          ON lv.viewer_id = me.sender_id
        WHERE me.ts BETWEEN :from_ts AND :to_ts
        GROUP BY lv.variant
    """)
    for row in conn.execute(conversation_sql, params):
        ensure(str(row.variant)).conversations = int(row.conversations or 0)

    report_sql = text(f"""
        SELECT lv.variant AS variant, COUNT(*) AS reports
        FROM user_report ur
        JOIN ({latest_variant_subquery()}) lv
          ON lv.viewer_id = ur.user_from_id
        WHERE ur.date BETWEEN :from_ts AND :to_ts
        GROUP BY lv.variant
    """)
    for row in conn.execute(report_sql, params):
        ensure(str(row.variant)).reports = int(row.reports or 0)

    block_sql = text(f"""
        SELECT lv.variant AS variant, COUNT(*) AS blocks
        FROM user_block ub
        JOIN ({latest_variant_subquery()}) lv
          ON lv.viewer_id = ub.user_from_id
        WHERE ub.date BETWEEN :from_ts AND :to_ts
        GROUP BY lv.variant
    """)
    for row in conn.execute(block_sql, params):
        ensure(str(row.variant)).blocks = int(row.blocks or 0)

    return metrics


def fetch_flag_row(conn, flag_name: str, segment_key: str):
    row = conn.execute(
        text("""
            SELECT id, enabled, json_config
            FROM feature_flags
            WHERE flag_name = :flag_name
              AND segment_key = :segment_key
            LIMIT 1
        """),
        {
            "flag_name": flag_name,
            "segment_key": segment_key,
        },
    ).mappings().first()
    return row


def evaluate_guardrails(
    control: VariantMetrics,
    treatment: VariantMetrics,
    max_conv_drop: float,
    max_report_increase: float,
    max_block_increase: float,
    max_top_decile_share_increase: float,
) -> Tuple[bool, Dict[str, float], List[str]]:
    deltas = {
        "conv_per_impression_change": relative_change(
            treatment.conversations_per_impression,
            control.conversations_per_impression,
        ),
        "report_rate_change": relative_change(
            treatment.report_rate_per_impression,
            control.report_rate_per_impression,
        ),
        "block_rate_change": relative_change(
            treatment.block_rate_per_impression,
            control.block_rate_per_impression,
        ),
        "top_decile_share_abs_increase": treatment.top_decile_share - control.top_decile_share,
    }

    failures: List[str] = []
    if deltas["conv_per_impression_change"] < -abs(max_conv_drop):
        failures.append("conversation_rate_drop")
    if deltas["report_rate_change"] > abs(max_report_increase):
        failures.append("report_rate_increase")
    if deltas["block_rate_change"] > abs(max_block_increase):
        failures.append("block_rate_increase")
    if deltas["top_decile_share_abs_increase"] > abs(max_top_decile_share_increase):
        failures.append("top_decile_concentration_increase")

    return len(failures) == 0, deltas, failures


def update_flag(
    conn,
    row_id: int,
    enabled: bool,
    config: Dict[str, object],
) -> None:
    conn.execute(
        text("""
            UPDATE feature_flags
            SET enabled = :enabled,
                json_config = :json_config
            WHERE id = :id
        """),
        {
            "id": row_id,
            "enabled": 1 if enabled else 0,
            "json_config": json.dumps(config, separators=(",", ":"), sort_keys=True),
        },
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--db-url", required=True)
    parser.add_argument("--flag-name", default="MATCH_RERANKER")
    parser.add_argument("--segment-key", default="*")
    parser.add_argument("--control-variant", default="control")
    parser.add_argument("--treatment-variant", default="treatment")
    parser.add_argument("--window-hours", type=int, default=24)
    parser.add_argument("--min-treatment-impressions", type=int, default=500)
    parser.add_argument("--max-conv-drop", type=float, default=0.05)
    parser.add_argument("--max-report-rate-increase", type=float, default=0.20)
    parser.add_argument("--max-block-rate-increase", type=float, default=0.20)
    parser.add_argument("--max-top-decile-share-increase", type=float, default=0.05)
    parser.add_argument("--rollout-stages", default="1,10,50,100")
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()

    now = datetime.now(timezone.utc)
    from_ts = now - timedelta(hours=max(1, int(args.window_hours)))
    to_ts = now

    engine = create_engine(args.db_url)
    with engine.begin() as conn:
        flag_row = fetch_flag_row(conn, args.flag_name, args.segment_key)
        if flag_row is None:
            result = {
                "ok": False,
                "reason": "feature_flag_not_found",
                "flag_name": args.flag_name,
                "segment_key": args.segment_key,
                "applied": False,
            }
            print(json.dumps(result, indent=2))
            return 1

        config = parse_json_config(flag_row.get("json_config"))
        current_traffic = int(clamp(int(config.get("trafficPercent", 0) or 0), 0, 100))
        if not bool(flag_row.get("enabled")):
            current_traffic = 0

        stages = parse_stages(args.rollout_stages)
        current_stage = pick_stage_for_percent(stages, current_traffic)

        by_variant = fetch_metric_counts(conn, from_ts, to_ts, args.segment_key)
        control = by_variant.get(args.control_variant, VariantMetrics())
        treatment = by_variant.get(args.treatment_variant, VariantMetrics())

        sufficient_data = treatment.impressions >= int(args.min_treatment_impressions)
        guardrail_passed, deltas, failures = evaluate_guardrails(
            control=control,
            treatment=treatment,
            max_conv_drop=args.max_conv_drop,
            max_report_increase=args.max_report_rate_increase,
            max_block_increase=args.max_block_rate_increase,
            max_top_decile_share_increase=args.max_top_decile_share_increase,
        )

        action = "hold"
        reason = "insufficient_data"
        target_traffic = current_stage

        if not sufficient_data:
            action = "hold"
            reason = "insufficient_data"
        elif not guardrail_passed:
            target_traffic = prev_stage(stages, current_stage)
            action = "rollback" if target_traffic < current_stage else "hold"
            reason = "guardrail_failed"
        else:
            candidate = next_stage(stages, current_stage)
            if candidate > current_stage:
                target_traffic = candidate
                action = "advance"
                reason = "guardrails_passed"
            else:
                action = "hold"
                reason = "max_stage_reached"

        applied = False
        if args.apply and action in {"advance", "rollback"}:
            config["trafficPercent"] = int(target_traffic)
            config["lastRolloutCheckAt"] = now.isoformat()
            config["lastRolloutAction"] = action
            config["lastRolloutReason"] = reason
            config["lastRolloutGuardrailFailures"] = failures
            update_flag(
                conn=conn,
                row_id=int(flag_row["id"]),
                enabled=(int(target_traffic) > 0),
                config=config,
            )
            applied = True

        output = {
            "ok": True,
            "flag_name": args.flag_name,
            "segment_key": args.segment_key,
            "window": {
                "from": from_ts.isoformat(),
                "to": to_ts.isoformat(),
                "hours": int(args.window_hours),
            },
            "current_traffic_percent": int(current_stage),
            "target_traffic_percent": int(target_traffic),
            "action": action,
            "reason": reason,
            "applied": applied,
            "sufficient_data": bool(sufficient_data),
            "guardrails_passed": bool(guardrail_passed),
            "guardrail_failures": failures,
            "deltas": deltas,
            "variants": {
                args.control_variant: control.to_dict(),
                args.treatment_variant: treatment.to_dict(),
            },
            "stages": stages,
        }
        print(json.dumps(output, indent=2))
        return 0


if __name__ == "__main__":
    raise SystemExit(main())
