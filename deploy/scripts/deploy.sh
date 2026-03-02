#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ROOT_DIR="$(cd "${DEPLOY_DIR}/.." && pwd)"
COMPOSE_FILE="${DEPLOY_DIR}/compose/bluegreen.yml"
ACTIVE_FILE="${DEPLOY_DIR}/ACTIVE_COLOR"
ENV_FILE="${ROOT_DIR}/.env.deploy"
NGINX_CONF_DIR="${DEPLOY_DIR}/nginx/conf.d"
BLUE_CONF="${NGINX_CONF_DIR}/active.conf"
GREEN_CONF="${NGINX_CONF_DIR}/active-green.conf"
CURRENT_CONF="${NGINX_CONF_DIR}/current.conf"
DRAIN_SECONDS="${DRAIN_SECONDS:-10}"
STOP_TIMEOUT_SECONDS="${STOP_TIMEOUT_SECONDS:-35}"
UP_TIMEOUT_SECONDS="${UP_TIMEOUT_SECONDS:-180}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo ".env.deploy 파일이 필요합니다: ${ENV_FILE}"
  exit 1
fi

for f in "${BLUE_CONF}" "${GREEN_CONF}"; do
  if [[ ! -f "${f}" ]]; then
    echo "nginx 라우팅 파일이 없습니다: ${f}"
    exit 1
  fi
done

dc() {
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" "$@"
}

dc_up_with_timeout() {
  local label="$1"
  shift

  if timeout "${UP_TIMEOUT_SECONDS}" docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d "$@"; then
    echo "[OK] ${label}"
  else
    echo "[FAIL] ${label} (timeout ${UP_TIMEOUT_SECONDS}s)"
    return 1
  fi
}

wait_with_retry() {
  local label="$1"
  local cmd="$2"
  local retries="${3:-30}"
  local sleep_sec="${4:-2}"

  for ((i=1; i<=retries; i++)); do
    if eval "${cmd}" >/dev/null 2>&1; then
      echo "[OK] ${label}"
      return 0
    fi
    echo "[WAIT] ${label} (${i}/${retries})"
    sleep "${sleep_sec}"
  done

  echo "[FAIL] ${label}"
  return 1
}

read_current_color() {
  if [[ -f "${ACTIVE_FILE}" ]]; then
    local color
    color="$(tr -d '[:space:]' < "${ACTIVE_FILE}")"
    if [[ "${color}" == "blue" || "${color}" == "green" ]]; then
      echo "${color}"
      return
    fi
  fi

  # 초기 배포 시 첫 타깃을 blue로 맞추기 위해 current를 green으로 간주
  echo "green"
}

determine_target_color() {
  local current="$1"
  if [[ "${current}" == "blue" ]]; then
    echo "green"
  else
    echo "blue"
  fi
}

switch_backend_route() {
  local target="$1"

  if [[ "${target}" == "blue" ]]; then
    cp "${BLUE_CONF}" "${CURRENT_CONF}"
  else
    cp "${GREEN_CONF}" "${CURRENT_CONF}"
  fi

  dc exec -T nginx nginx -s reload
  echo "${target}" > "${ACTIVE_FILE}"
  echo "활성 백엔드 전환 완료: ${target}"
}

CURRENT_COLOR="$(read_current_color)"
TARGET_COLOR="$(determine_target_color "${CURRENT_COLOR}")"

echo "현재 활성 색상: ${CURRENT_COLOR}"
echo "배포 대상 색상: ${TARGET_COLOR}"

echo "기본 서비스 확인(mysql/frontend/nginx)..."
dc_up_with_timeout "base services up" mysql frontend nginx

echo "대상 백엔드 기동..."
dc_up_with_timeout "backend-${TARGET_COLOR} up" "backend-${TARGET_COLOR}"

echo "대상 백엔드 헬스체크..."
wait_with_retry \
  "backend-${TARGET_COLOR} /actuator/health" \
  "dc exec -T nginx sh -lc 'wget --tries=1 --timeout=2 -qO- http://backend-${TARGET_COLOR}:8080/actuator/health | grep -q UP'"

echo "프론트엔드 헬스체크..."
wait_with_retry \
  "frontend /" \
  "dc exec -T nginx sh -lc 'wget --tries=1 --timeout=2 -qO- http://frontend:3000 > /dev/null'"

echo "트래픽 전환..."
switch_backend_route "${TARGET_COLOR}"

echo "전환 후 nginx 응답 확인..."
wait_with_retry \
  "nginx / 응답" \
  "dc exec -T nginx sh -lc 'wget --tries=1 --timeout=2 -qO- http://localhost > /dev/null'" \
  10 \
  1

if [[ "${CURRENT_COLOR}" == "blue" || "${CURRENT_COLOR}" == "green" ]]; then
  if [[ "${CURRENT_COLOR}" != "${TARGET_COLOR}" ]]; then
    echo "기존 백엔드 드레이닝(${DRAIN_SECONDS}s) 후 정리..."
    sleep "${DRAIN_SECONDS}"
    dc stop -t "${STOP_TIMEOUT_SECONDS}" "backend-${CURRENT_COLOR}" || true
  fi
fi

echo "배포 완료: ${CURRENT_COLOR} -> ${TARGET_COLOR}"
