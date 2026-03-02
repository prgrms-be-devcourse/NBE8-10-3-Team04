# AWS EC2 기반 단순화 Blue/Green 배포 가이드

## 1. 개요

- 프로젝트: Spring Boot(Backend) + Next.js(Frontend) + MySQL
- 인프라: AWS EC2 VM 1대
- 배포 방식: Docker Compose + Nginx + Backend Blue/Green
- CI/CD: GitHub Actions (`main` push 또는 `workflow_dispatch`)
- 외부 진입: `http://<PUBLIC_IP>:80`

핵심 원칙:
- Blue/Green은 **backend만** 적용
- `frontend`, `mysql`, `nginx`는 상시 유지
- `/api` 경로만 무중단 전환

---

## 2. 레포 내 배포 파일

### 2.1 Compose
- `/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/deploy/compose/bluegreen.yml`
  - `mysql`
  - `backend-blue`, `backend-green`
  - `frontend` (단일)
  - `nginx`

### 2.2 Nginx
- `/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/deploy/nginx/nginx.conf`
- `/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/deploy/nginx/conf.d/current.conf`
- `/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/deploy/nginx/conf.d/active.conf` (backend-blue)
- `/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/deploy/nginx/conf.d/active-green.conf` (backend-green)

### 2.3 배포 스크립트
- `/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/deploy/scripts/deploy.sh`
  - 단일 배포 진입점
  - 내부에서 헬스체크 + 라우팅 전환 + 이전 backend 정리 수행

### 2.4 GitHub Actions
- `/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/.github/workflows/deploy.yml`

---

## 3. 서버 사전 준비 (AWS EC2)

### 3.1 포트 정책
- 인바운드 허용: `22/tcp`, `80/tcp`
- 외부 미오픈: `3306`, `8080`, `3000`

### 3.2 서버 기본 세팅
```bash
sudo dnf -y update
sudo dnf -y install git docker
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
```

```bash
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -fSL https://github.com/docker/compose/releases/download/v2.40.3/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
docker --version
docker compose version
```

### 3.3 프로젝트 배치
GitHub Actions SSH 배포 경로와 동일하게 맞춥니다.

```bash
mkdir -p /home/ec2-user/app
cd /home/ec2-user/app
git clone <YOUR_REPO_URL> .
chmod +x deploy/scripts/deploy.sh
```

---

## 4. `.env.deploy` 구성

서버 루트(`repo root`)에 `.env.deploy` 파일을 둡니다.

필수 키:
- `BACKEND_IMAGE`
- `FRONTEND_IMAGE`
- `NEXT_PUBLIC_API_BASE_URL`
- `MYSQL_DATABASE`
- `MYSQL_ROOT_PASSWORD`
- `MYSQL_APP_USER`
- `MYSQL_APP_PASSWORD`
- `AWS_ACCESS_KEY`
- `AWS_SECRET_KEY`
- `AWS_BUCKET_NAME`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `JWT_SECRET_KEY`
- `GEMINI_API_KEY`

주의:
- `.env.deploy`는 커밋 금지(`.gitignore` 반영)
- 실제 비밀값은 GitHub Secrets/서버 파일에서만 관리

---

## 5. 자동 배포 흐름

1. `main` 브랜치에 push (또는 `workflow_dispatch`로 수동 실행)
2. GitHub Actions가 backend/frontend 이미지 빌드 후 GHCR push
3. 워크플로우가 서버 SSH 접속
4. 서버 `.env.deploy` 갱신
5. `./deploy/scripts/deploy.sh` 실행
   - 기본 서비스(`mysql`, `frontend`, `nginx`) 유지/보장
   - 반대 color backend만 기동
   - backend/frontend 헬스체크
   - Nginx `/api` 라우팅 전환
   - 기존 backend 10초 드레이닝 후 stop

---

## 6. 수동 운영 명령어

### 6.1 초기 기동
```bash
docker compose --env-file .env.deploy -f deploy/compose/bluegreen.yml up -d mysql frontend nginx backend-blue
cp deploy/nginx/conf.d/active.conf deploy/nginx/conf.d/current.conf
docker compose --env-file .env.deploy -f deploy/compose/bluegreen.yml exec -T nginx nginx -s reload
echo blue > deploy/ACTIVE_COLOR
```

### 6.2 현재 상태 확인
```bash
docker compose --env-file .env.deploy -f deploy/compose/bluegreen.yml ps
cat deploy/ACTIVE_COLOR
```

### 6.3 수동 배포
```bash
./deploy/scripts/deploy.sh
```

### 6.4 수동 롤백 (스크립트 없이 즉시 복구)
현재 active를 확인한 뒤 반대 색상으로 되돌립니다.

```bash
# 예: blue로 롤백
cp deploy/nginx/conf.d/active.conf deploy/nginx/conf.d/current.conf
docker compose --env-file .env.deploy -f deploy/compose/bluegreen.yml exec -T nginx nginx -s reload
echo blue > deploy/ACTIVE_COLOR

# 예: green으로 롤백
cp deploy/nginx/conf.d/active-green.conf deploy/nginx/conf.d/current.conf
docker compose --env-file .env.deploy -f deploy/compose/bluegreen.yml exec -T nginx nginx -s reload
echo green > deploy/ACTIVE_COLOR
```

---

## 7. GitHub Secrets 목록

배포 워크플로우에서 사용하는 키:

- `AWS_SERVER_HOST`
- `AWS_SERVER_SSH_KEY`
- `MYSQL_DATABASE`
- `MYSQL_ROOT_PASSWORD`
- `MYSQL_APP_USER`
- `MYSQL_APP_PASSWORD`
- `AWS_ACCESS_KEY`
- `AWS_SECRET_KEY`
- `AWS_BUCKET_NAME`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `JWT_SECRET_KEY`
- `GEMINI_API_KEY`

참고:
- `NEXT_PUBLIC_API_BASE_URL`은 현재 워크플로우 파일 내 `PUBLIC_API_BASE_URL` 환경값으로 관리

---

## 8. 트러블슈팅

### 8.1 `docker compose config` 변수 누락
- 원인: `.env.deploy` 미존재 또는 키 누락
- 확인:
```bash
docker compose --env-file .env.deploy -f deploy/compose/bluegreen.yml config
```

### 8.2 전환 후 API 오류(502/5xx)
- 원인: 대상 backend 헬스체크 전환 타이밍 문제
- 조치: 즉시 수동 롤백(6.4) 후 대상 backend 로그 확인
```bash
docker compose --env-file .env.deploy -f deploy/compose/bluegreen.yml logs --tail=200 backend-blue
docker compose --env-file .env.deploy -f deploy/compose/bluegreen.yml logs --tail=200 backend-green
```

### 8.3 `nginx -s reload` 실패
- 원인: `current.conf` 문법/경로 문제
- 조치:
```bash
docker compose --env-file .env.deploy -f deploy/compose/bluegreen.yml exec -T nginx nginx -t
```

---

## 9. 운영 체크포인트

- DB(`mysql`)는 상태형 서비스이므로 배포마다 재시작하지 않기
- 인바운드 최소 포트(22, 80) 유지
- 불필요 컨테이너/이미지 정리
- AWS Budget/알람 설정
- `deploy/ACTIVE_COLOR`와 배포 로그 주기 확인
