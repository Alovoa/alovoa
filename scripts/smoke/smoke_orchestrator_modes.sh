#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.e2e.yml}"
STACK_SERVICES=(aura-app media-service)
HEALTH_URL="${HEALTH_URL:-http://localhost:8080/actuator/health}"
STATUS_URL="${STATUS_URL:-http://localhost:8080/api/v1/ml/integrations/status}"

MODE="${1:-all}"

wait_for_health() {
  local retries=60
  local sleep_sec=3
  for ((i=1; i<=retries; i++)); do
    if curl -fsS "$HEALTH_URL" >/dev/null 2>&1; then
      return 0
    fi
    sleep "$sleep_sec"
  done
  echo "Timed out waiting for app health: $HEALTH_URL" >&2
  return 1
}

run_mode() {
  local mode="$1"
  local qdrantEnabled="false"
  local clipEnabled="false"
  local faceQualityAdapterEnabled="false"
  if [[ "$mode" == "local" ]]; then
    qdrantEnabled="true"
    clipEnabled="true"
    faceQualityAdapterEnabled="true"
  fi

  echo "=== Smoke mode: $mode ==="
  (
    cd "$ROOT_DIR"
    APP_AURA_ML_QDRANT_ENABLED="$qdrantEnabled" \
    NSFW_USE_CLIP_NSFW="$clipEnabled" \
    FACE_QUALITY_USE_ADAPTER="$faceQualityAdapterEnabled" \
      docker compose -f "$COMPOSE_FILE" up -d --build "${STACK_SERVICES[@]}"
  )

  wait_for_health

  local status_payload
  status_payload="$(curl -fsS "$STATUS_URL")"
  echo "$status_payload" | grep -q "\"qdrantEnabled\":${qdrantEnabled}" || {
    echo "qdrantEnabled flag mismatch for mode=$mode" >&2
    return 1
  }

  local bad_req_status
  bad_req_status="$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/api/v1/ml/integrations/qdrant/candidate-enrichment")"
  if [[ "$bad_req_status" != "400" ]]; then
    echo "Expected 400 for invalid enrichment request, got $bad_req_status" >&2
    return 1
  fi

  (
    cd "$ROOT_DIR"
    docker compose -f "$COMPOSE_FILE" rm -sf "${STACK_SERVICES[@]}" >/dev/null 2>&1 || true
  )
}

cleanup() {
  (
    cd "$ROOT_DIR"
    docker compose -f "$COMPOSE_FILE" rm -sf "${STACK_SERVICES[@]}" >/dev/null 2>&1 || true
  )
}
trap cleanup EXIT

case "$MODE" in
  proxy)
    run_mode "proxy"
    ;;
  local)
    run_mode "local"
    ;;
  all)
    run_mode "proxy"
    run_mode "local"
    ;;
  *)
    echo "Usage: $0 [proxy|local|all]" >&2
    exit 1
    ;;
esac

echo "Smoke checks passed for mode=${MODE}"
