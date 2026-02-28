#!/usr/bin/env bash
set -euo pipefail

TARGET_COLOR="${1:-}"
if [[ "${TARGET_COLOR}" != "blue" && "${TARGET_COLOR}" != "green" ]]; then
  echo "사용법: $0 <blue|green>"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
NGINX_CONF_DIR="${DEPLOY_DIR}/nginx/conf.d"
COMPOSE_FILE="${DEPLOY_DIR}/compose/bluegreen.yml"

if [[ "${TARGET_COLOR}" == "blue" ]]; then
  cp "${NGINX_CONF_DIR}/active.conf" "${NGINX_CONF_DIR}/current.conf"
else
  cp "${NGINX_CONF_DIR}/active-green.conf" "${NGINX_CONF_DIR}/current.conf"
fi

docker compose --env-file .env.deploy -f "${COMPOSE_FILE}" exec -T nginx nginx -s reload

echo "${TARGET_COLOR}" > "${DEPLOY_DIR}/ACTIVE_COLOR"
echo "활성 색상 전환 완료: ${TARGET_COLOR}"
