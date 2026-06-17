#!/bin/bash
set -euo pipefail

HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8080/health}"

for _ in {1..30}; do
  if curl -fsS "${HEALTH_URL}" >/dev/null; then
    exit 0
  fi
  sleep 2
done

echo "Health check failed: ${HEALTH_URL}" >&2
exit 1
