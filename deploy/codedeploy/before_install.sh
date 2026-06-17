#!/bin/bash
set -euo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/paylens/codedeploy}"
BIN_DIR="${BIN_DIR:-/opt/paylens/bin}"

mkdir -p "${DEPLOY_ROOT}" "${BIN_DIR}"
