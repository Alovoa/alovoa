#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RUN_DIR="${ROOT_DIR}/.run/local-ux-stack"
LOG_DIR="${ROOT_DIR}/logs/local-ux-stack"
mkdir -p "${RUN_DIR}" "${LOG_DIR}"

MEDIA_AI_PID_FILE="${RUN_DIR}/mock-services.pid"
BACKEND_PID_FILE="${RUN_DIR}/backend.pid"

wait_for_health() {
  local name="$1"
  local url="$2"
  local timeout="${3:-90}"
  local start_ts
  start_ts="$(date +%s)"
  while true; do
    local code
    code="$(curl -s -o /dev/null -w "%{http_code}" "${url}" || true)"
    if [[ "${code}" == "200" ]]; then
      echo "[ok] ${name} healthy at ${url}"
      return 0
    fi
    local now
    now="$(date +%s)"
    if (( now - start_ts >= timeout )); then
      echo "[fail] ${name} did not become healthy within ${timeout}s (${url})" >&2
      return 1
    fi
    sleep 2
  done
}

if [[ -f "${MEDIA_AI_PID_FILE}" ]] && kill -0 "$(cat "${MEDIA_AI_PID_FILE}")" 2>/dev/null; then
  echo "[skip] Mock media/ai services already running (pid $(cat "${MEDIA_AI_PID_FILE}"))"
else
  echo "[start] Launching mock media-service and ai-service"
  nohup python3 "${ROOT_DIR}/scripts/dev/mock_aux_services.py" >"${LOG_DIR}/mock-services.log" 2>&1 &
  echo $! > "${MEDIA_AI_PID_FILE}"
fi

if [[ -f "${BACKEND_PID_FILE}" ]] && kill -0 "$(cat "${BACKEND_PID_FILE}")" 2>/dev/null; then
  echo "[skip] Java backend already running (pid $(cat "${BACKEND_PID_FILE}"))"
else
  echo "[start] Launching Java backend on profile=local"
  if [[ -x /usr/libexec/java_home ]]; then
    export JAVA_HOME="$(
      /usr/libexec/java_home -v 17 2>/dev/null \
      || /usr/libexec/java_home -v 21 2>/dev/null \
      || /usr/libexec/java_home
    )"
    export PATH="${JAVA_HOME}/bin:${PATH}"
  fi
  nohup mvn -q -DskipTests -f "${ROOT_DIR}/pom.xml" spring-boot:run \
    -Dspring-boot.run.profiles=local >"${LOG_DIR}/backend.log" 2>&1 &
  echo $! > "${BACKEND_PID_FILE}"
fi

wait_for_health "media-service" "http://localhost:8001/health" 30
wait_for_health "ai-service" "http://localhost:8002/health" 30
wait_for_health "aura-app" "http://localhost:8080/actuator/health" 120

echo
echo "Local UX stack is up:"
echo "  - http://localhost:8080/actuator/health"
echo "  - http://localhost:8001/health"
echo "  - http://localhost:8002/health"
echo
echo "To stop: ${ROOT_DIR}/scripts/dev/stop_local_ux_stack.sh"
