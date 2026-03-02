#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ROOT_DIR="$(cd "${DEPLOY_DIR}/.." && pwd)"
COMPOSE_FILE="${DEPLOY_DIR}/compose/bluegreen.yml"
ENV_FILE="${ROOT_DIR}/.env.deploy"
NGINX_CONF_DIR="${DEPLOY_DIR}/nginx/conf.d"
BLUE_CONF="${NGINX_CONF_DIR}/active.conf"
CURRENT_CONF="${NGINX_CONF_DIR}/current.conf"
ACTIVE_FILE="${DEPLOY_DIR}/ACTIVE_COLOR"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo ".env.deploy 파일이 필요합니다: ${ENV_FILE}"
  exit 1
fi

if [[ ! -f "${BLUE_CONF}" ]]; then
  echo "nginx 라우팅 파일이 없습니다: ${BLUE_CONF}"
  exit 1
fi

dc() {
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" "$@"
}

echo "[1/5] nginx 라우팅을 blue로 고정"
cp "${BLUE_CONF}" "${CURRENT_CONF}"
echo "blue" > "${ACTIVE_FILE}"

echo "[2/5] 이미지 pull"
dc pull mysql backend-blue frontend nginx

echo "[3/5] 서비스 실행 (단순 배포 모드)"
dc up -d mysql backend-blue frontend nginx

echo "[4/5] green 백엔드는 중지 (리소스 절약)"
dc stop backend-green >/dev/null 2>&1 || true

echo "[5/5] 상태 확인"
dc ps
echo "단순 배포 완료 (blue 고정)"
