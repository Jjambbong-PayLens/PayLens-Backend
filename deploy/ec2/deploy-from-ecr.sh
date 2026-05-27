#!/bin/bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <repository-uri> <image-tag>" >&2
  exit 1
fi

REPOSITORY_URI="$1"
IMAGE_TAG="$2"

AWS_REGION="${AWS_REGION:-ap-northeast-2}"
ENV_FILE="${ENV_FILE:-/opt/paylens/app/.env}"
CONTAINER_NAME="${CONTAINER_NAME:-paylens-backend}"
HOST_PORT="${HOST_PORT:-8080}"
CONTAINER_PORT="${CONTAINER_PORT:-8080}"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:${HOST_PORT}/health}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing env file: ${ENV_FILE}" >&2
  exit 1
fi

REGISTRY_HOST="${REPOSITORY_URI%%/*}"
FULL_IMAGE="${REPOSITORY_URI}:${IMAGE_TAG}"

aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${REGISTRY_HOST}"

docker pull "${FULL_IMAGE}"

if docker ps --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
  docker stop "${CONTAINER_NAME}"
fi

if docker ps -a --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
  docker rm "${CONTAINER_NAME}"
fi

docker run -d \
  --name "${CONTAINER_NAME}" \
  --restart unless-stopped \
  --env-file "${ENV_FILE}" \
  -p "${HOST_PORT}:${CONTAINER_PORT}" \
  "${FULL_IMAGE}"

for _ in {1..30}; do
  if curl -fsS "${HEALTH_URL}" >/dev/null; then
    docker image prune -f >/dev/null 2>&1 || true
    exit 0
  fi
  sleep 2
done

docker logs "${CONTAINER_NAME}" || true
echo "Container did not become healthy in time." >&2
exit 1
