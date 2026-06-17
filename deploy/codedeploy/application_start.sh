#!/bin/bash
set -euo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/paylens/codedeploy}"
IMAGE_REF_FILE="${IMAGE_REF_FILE:-${DEPLOY_ROOT}/image-uri.txt}"
DEPLOY_SCRIPT="${DEPLOY_SCRIPT:-/opt/paylens/bin/deploy-from-ecr.sh}"

if [[ ! -f "${IMAGE_REF_FILE}" ]]; then
  echo "Missing image URI file: ${IMAGE_REF_FILE}" >&2
  exit 1
fi

IMAGE_REF="$(tr -d '[:space:]' < "${IMAGE_REF_FILE}")"
if [[ -z "${IMAGE_REF}" || "${IMAGE_REF}" != *":"* ]]; then
  echo "Invalid image URI: ${IMAGE_REF}" >&2
  exit 1
fi

REPOSITORY_URI="${IMAGE_REF%:*}"
IMAGE_TAG="${IMAGE_REF##*:}"

AWS_REGION="${AWS_REGION:-ap-northeast-2}" CONTAINER_NAME="${CONTAINER_NAME:-paylens-backend}" HOST_PORT="${HOST_PORT:-8080}" CONTAINER_PORT="${CONTAINER_PORT:-8080}" ENV_FILE="${ENV_FILE:-/opt/paylens/app/.env}" HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8080/health}" "${DEPLOY_SCRIPT}" "${REPOSITORY_URI}" "${IMAGE_TAG}"
