#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RUN_DIR="${ROOT_DIR}/.run/local-ux-stack"

stop_pid_file() {
  local pid_file="$1"
  local label="$2"
  if [[ ! -f "${pid_file}" ]]; then
    echo "[skip] ${label} pid file not found"
    return 0
  fi
  local pid
  pid="$(cat "${pid_file}")"
  if kill -0 "${pid}" 2>/dev/null; then
    echo "[stop] ${label} (pid ${pid})"
    kill "${pid}" || true
  else
    echo "[skip] ${label} process already stopped (pid ${pid})"
  fi
  rm -f "${pid_file}"
}

stop_pid_file "${RUN_DIR}/backend.pid" "java-backend"
stop_pid_file "${RUN_DIR}/mock-services.pid" "mock-media-ai"
