#!/bin/bash
set -euo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/paylens/codedeploy}"
BIN_DIR="${BIN_DIR:-/opt/paylens/bin}"
DEPLOY_SCRIPT="${DEPLOY_ROOT}/deploy/ec2/deploy-from-ecr.sh"

if [[ ! -f "${DEPLOY_SCRIPT}" ]]; then
  echo "Missing deploy script: ${DEPLOY_SCRIPT}" >&2
  exit 1
fi

chmod +x "${DEPLOY_ROOT}"/deploy/codedeploy/*.sh
install -m 750 "${DEPLOY_SCRIPT}" "${BIN_DIR}/deploy-from-ecr.sh"
