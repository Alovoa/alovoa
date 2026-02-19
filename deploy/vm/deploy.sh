#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [[ ! -f .env ]]; then
  echo "Missing deploy/vm/.env"
  echo "Create it from deploy/vm/.env.example before deploying."
  exit 1
fi

echo "Pulling latest images..."
docker compose --env-file .env -f docker-compose.vm.yml pull

echo "Starting or updating stack..."
docker compose --env-file .env -f docker-compose.vm.yml up -d --remove-orphans

echo "Current service status:"
docker compose --env-file .env -f docker-compose.vm.yml ps
