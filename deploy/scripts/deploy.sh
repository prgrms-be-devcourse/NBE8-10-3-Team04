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

wait_for_service() {
  local service="$1"
  local timeout="${2:-180}"
  local waited=0
  local cid
  local status

  cid="$(dc ps -q "${service}" 2>/dev/null || true)"
  if [[ -z "${cid}" ]]; then
    echo "[ERROR] ${service} 컨테이너를 찾을 수 없습니다."
    return 1
  fi

  while (( waited < timeout )); do
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${cid}" 2>/dev/null || echo unknown)"

    case "${status}" in
      healthy|running)
        echo "[OK] ${service} 상태: ${status}"
        return 0
        ;;
      unhealthy|exited|dead)
        echo "[ERROR] ${service} 상태: ${status}"
        dc logs --tail 120 "${service}" || true
        return 1
        ;;
      *)
        sleep 5
        waited=$((waited + 5))
        ;;
    esac
  done

  echo "[ERROR] ${service} 대기 시간 초과 (${timeout}s)"
  dc logs --tail 120 "${service}" || true
  return 1
}

echo "[1/8] nginx 라우팅을 blue로 고정"
cp "${BLUE_CONF}" "${CURRENT_CONF}"
echo "blue" > "${ACTIVE_FILE}"

echo "[2/8] 이미지 pull (순차)"
dc pull mysql
dc pull backend-blue
dc pull frontend
dc pull nginx

echo "[3/8] mysql 기동"
dc up -d mysql
wait_for_service mysql 240

echo "[4/8] backend-blue 기동"
dc up -d backend-blue
wait_for_service backend-blue 240

echo "[5/8] frontend 기동"
dc up -d frontend
wait_for_service frontend 180

echo "[6/8] nginx 기동"
dc up -d nginx
wait_for_service nginx 120

echo "[7/8] green 백엔드는 중지 (리소스 절약)"
dc stop backend-green >/dev/null 2>&1 || true

echo "[8/8] 상태 확인"
dc ps
echo "순차 배포 완료 (blue 고정)"
