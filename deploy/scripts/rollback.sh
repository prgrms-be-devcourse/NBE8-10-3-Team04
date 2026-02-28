#!/usr/bin/env bash
set -euo pipefail

TARGET_COLOR="${1:-}"
if [[ "${TARGET_COLOR}" != "blue" && "${TARGET_COLOR}" != "green" ]]; then
  echo "사용법: $0 <blue|green>"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SWITCH_SCRIPT="${SCRIPT_DIR}/switch.sh"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ROOT_DIR="$(cd "${DEPLOY_DIR}/.." && pwd)"
COMPOSE_FILE="${DEPLOY_DIR}/compose/bluegreen.yml"

dc() {
  docker compose --env-file "${ROOT_DIR}/.env.deploy" -f "${COMPOSE_FILE}" "$@"
}

"${SWITCH_SCRIPT}" "${TARGET_COLOR}"

if [[ "${TARGET_COLOR}" == "blue" ]]; then
  dc stop backend-green frontend-green || true
else
  dc stop backend-blue frontend-blue || true
fi

echo "롤백 완료: ${TARGET_COLOR}"
