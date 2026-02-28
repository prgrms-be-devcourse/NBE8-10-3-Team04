#!/usr/bin/env bash
set -euo pipefail

TARGET_COLOR="${1:-}"
if [[ "${TARGET_COLOR}" != "blue" && "${TARGET_COLOR}" != "green" ]]; then
  echo "사용법: $0 <blue|green>"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ROOT_DIR="$(cd "${DEPLOY_DIR}/.." && pwd)"
COMPOSE_FILE="${DEPLOY_DIR}/compose/bluegreen.yml"

dc() {
  docker compose --env-file "${ROOT_DIR}/.env.deploy" -f "${COMPOSE_FILE}" "$@"
}

echo "backend-${TARGET_COLOR} 헬스체크..."
dc exec -T nginx sh -lc "wget -qO- http://backend-${TARGET_COLOR}:8080/actuator/health | grep -q UP"

echo "frontend-${TARGET_COLOR} 헬스체크..."
dc exec -T nginx sh -lc "wget -qO- http://frontend-${TARGET_COLOR}:3000 > /dev/null"

echo "헬스체크 통과: ${TARGET_COLOR}"
