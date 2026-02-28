# AWS EC2 기반 Blue/Green 배포 가이드

## 1. 개요

- 프로젝트: Spring Boot(Backend) + Next.js(Frontend) + MySQL
- 인프라: AWS EC2 VM 1대
- 배포 방식: Docker Compose + Nginx + Blue/Green
- CI/CD: GitHub Actions (`main` push 시 자동 배포)
- 외부 진입: `http://<PUBLIC_IP>:80` (도메인/HTTPS는 추후 확장)

---

## 2. 레포 내 배포 파일

### 2.1 Compose
- [`deploy/compose/bluegreen.yml`](/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/deploy/compose/bluegreen.yml)
  - `mysql`
  - `backend-blue`, `backend-green`
  - `frontend-blue`, `frontend-green`
  - `nginx`

### 2.2 Nginx
- [`deploy/nginx/nginx.conf`](/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/deploy/nginx/nginx.conf)
- [`deploy/nginx/conf.d/current.conf`](/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/deploy/nginx/conf.d/current.conf)
- [`deploy/nginx/conf.d/active.conf`](/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/deploy/nginx/conf.d/active.conf) (blue)
- [`deploy/nginx/conf.d/active-green.conf`](/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/deploy/nginx/conf.d/active-green.conf) (green)

### 2.3 배포 스크립트
- [`deploy/scripts/deploy.sh`](/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/deploy/scripts/deploy.sh): 자동 배포(반대 color 기동 → 체크 → 전환)
- [`deploy/scripts/switch.sh`](/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/deploy/scripts/switch.sh): active color 전환
- [`deploy/scripts/healthcheck.sh`](/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/deploy/scripts/healthcheck.sh): color 헬스체크
- [`deploy/scripts/rollback.sh`](/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/deploy/scripts/rollback.sh): 수동 롤백

### 2.4 GitHub Actions
- [`.github/workflows/deploy.yml`](/Users/chan/Desktop/Programmers/thirdProject/NBE8-10-3-Team04/.github/workflows/deploy.yml)

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
```bash
mkdir -p ~/apps/team04
cd ~/apps/team04
git clone <YOUR_REPO_URL> .
chmod +x deploy/scripts/*.sh
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
- `.env.deploy`는 커밋 금지(`.gitignore` 반영됨)
- 실제 비밀값은 GitHub Secrets/서버 파일에서만 관리

---

## 5. 자동 배포 흐름

1. `main` 브랜치에 push
2. GitHub Actions가 BE/FE 이미지 빌드 및 GHCR push
3. 워크플로우가 서버 SSH 접속
4. 서버 `.env.deploy` 갱신
5. `./deploy/scripts/deploy.sh` 실행
   - 현재 active color 확인
   - 반대 color 기동
   - backend/frontend 헬스체크
   - 성공 시 `switch.sh`로 Nginx 전환
   - 이전 color 정리

---

## 6. 수동 운영 명령어

### 6.1 전체 기동
```bash
docker compose --env-file .env.deploy -f deploy/compose/bluegreen.yml up -d
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

### 6.4 수동 전환
```bash
./deploy/scripts/switch.sh blue
./deploy/scripts/switch.sh green
```

### 6.5 수동 롤백
```bash
./deploy/scripts/rollback.sh blue
./deploy/scripts/rollback.sh green
```

### 6.6 헬스체크
```bash
./deploy/scripts/healthcheck.sh blue
./deploy/scripts/healthcheck.sh green
```

---

## 7. GitHub Secrets 목록

배포 워크플로우에서 사용하는 키:

- `AWS_SERVER_HOST`
- `AWS_SERVER_SSH_KEY`
- `NEXT_PUBLIC_API_BASE_URL` (현재 워크플로우 하드코딩, 공인 IP 변경 시 워크플로우 값 수정 필요)
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

---

## 8. 트러블슈팅

### 8.1 `docker compose config`에서 변수 누락 에러
- 원인: `.env.deploy` 미존재 또는 키 누락
- 확인:
```bash
docker compose --env-file .env.deploy -f deploy/compose/bluegreen.yml config
```

### 8.2 전환 후 502 발생
- 원인: 대상 color 헬스체크 미통과 상태에서 전환
- 조치:
```bash
./deploy/scripts/healthcheck.sh <blue|green>
./deploy/scripts/rollback.sh <이전색상>
```

### 8.3 `nginx -s reload` 실패
- 원인: `current.conf` 문법/경로 문제
- 조치:
```bash
docker compose --env-file .env.deploy -f deploy/compose/bluegreen.yml exec -T nginx nginx -t
```

---

## 9. 비용 운영 체크포인트

- 프리 티어/크레딧 범위 외 리소스 생성 금지
- 인바운드 최소 포트(22, 80) 유지
- 불필요 컨테이너/이미지 정리
- 용량/트래픽/알림 정책(AWS Budget) 설정
- 배포 로그와 상태 파일(`deploy/ACTIVE_COLOR`) 주기 확인
