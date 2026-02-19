#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${MYSQL_HOST:-}" || -z "${MYSQL_USER:-}" || -z "${MYSQL_DB:-}" ]]; then
  echo "Set MYSQL_HOST, MYSQL_USER, MYSQL_DB and optionally MYSQL_PASSWORD, MYSQL_PORT before running."
  exit 1
fi

MYSQL_PORT="${MYSQL_PORT:-3306}"

if [[ -n "${MYSQL_PASSWORD:-}" ]]; then
  export MYSQL_PWD="$MYSQL_PASSWORD"
fi

echo "Running reranker event backfill..."
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" "$MYSQL_DB" < scripts/sql/backfill_reranker_events.sql

echo "Backfill complete. Trigger rolling stats rebuild via admin endpoint:"
echo "POST /api/v1/admin/matching-analytics/rebuild-stats"
