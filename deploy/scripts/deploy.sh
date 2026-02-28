#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ROOT_DIR="$(cd "${DEPLOY_DIR}/.." && pwd)"
COMPOSE_FILE="${DEPLOY_DIR}/compose/bluegreen.yml"
ACTIVE_FILE="${DEPLOY_DIR}/ACTIVE_COLOR"
SWITCH_SCRIPT="${SCRIPT_DIR}/switch.sh"

if [[ ! -f "${ROOT_DIR}/.env.deploy" ]]; then
  echo ".env.deploy 파일이 필요합니다: ${ROOT_DIR}/.env.deploy"
  exit 1
fi

if [[ ! -x "${SWITCH_SCRIPT}" ]]; then
  echo "switch.sh 실행 권한이 없습니다: ${SWITCH_SCRIPT}"
  exit 1
fi

CURRENT_COLOR="blue"
if [[ -f "${ACTIVE_FILE}" ]]; then
  CURRENT_COLOR="$(tr -d '[:space:]' < "${ACTIVE_FILE}")"
fi

if [[ "${CURRENT_COLOR}" == "blue" ]]; then
  TARGET_COLOR="green"
else
  TARGET_COLOR="blue"
fi

echo "현재 활성 색상: ${CURRENT_COLOR}"
echo "배포 대상 색상: ${TARGET_COLOR}"

dc() {
  docker compose --env-file "${ROOT_DIR}/.env.deploy" -f "${COMPOSE_FILE}" "$@"
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

echo "대상 색상 서비스 기동..."
dc up -d mysql "backend-${TARGET_COLOR}" "frontend-${TARGET_COLOR}" nginx

echo "백엔드 헬스체크..."
wait_with_retry \
  "backend-${TARGET_COLOR} /actuator/health" \
  "dc exec -T nginx sh -lc 'wget -qO- http://backend-${TARGET_COLOR}:8080/actuator/health | grep -q UP'"

echo "프론트엔드 헬스체크..."
wait_with_retry \
  "frontend-${TARGET_COLOR} /" \
  "dc exec -T nginx sh -lc 'wget -qO- http://frontend-${TARGET_COLOR}:3000 > /dev/null'"

echo "활성 색상 전환..."
"${SWITCH_SCRIPT}" "${TARGET_COLOR}"

echo "전환 후 기본 응답 확인..."
wait_with_retry \
  "nginx / 응답" \
  "dc exec -T nginx sh -lc 'wget -qO- http://localhost > /dev/null'" \
  10 \
  1

echo "이전 색상 정리..."
dc stop "backend-${CURRENT_COLOR}" "frontend-${CURRENT_COLOR}" || true

echo "배포 완료: ${CURRENT_COLOR} -> ${TARGET_COLOR}"
