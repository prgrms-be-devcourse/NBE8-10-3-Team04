import http from 'k6/http';
import { check, sleep, fail } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 150 },
    { duration: '20s', target: 150 },
    { duration: '10s', target: 300 },  // 최대 300명으로 증가
    { duration: '20s', target: 300 },
    { duration: '10s', target: 0 },
  ],
};

const BASE_URL = __ENV.BASE_URL;
const USER_ID = __ENV.USER_ID;
const USER_PW = __ENV.USER_PW;

if (!BASE_URL) {
  throw new Error('환경변수 BASE_URL이 설정되어 있지 않습니다. .env 파일을 추가하고 --env-file 옵션으로 로드하세요.');
}
if (!USER_ID || !USER_PW) {
  throw new Error('로그인용 USER_ID, USER_PW 환경변수가 필요합니다.');
}

function loginAndGetAuth() {
  const loginRes = http.post(
    `${BASE_URL}/api/v1/user/login`,
    JSON.stringify({ loginId: USER_ID, password: USER_PW }),
    {
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
    },
  );

  const loginOk = check(loginRes, { 'login status < 400': (r) => r.status < 400 });
  if (!loginOk) {
    fail(`로그인 실패 (status: ${loginRes.status})`);
  }

  // 세션 쿠키 정리
  const cookies = Object.fromEntries(
    Object.entries(loginRes.cookies || {}).map(([name, values]) => [name, values?.[0]?.value]),
  );

  // 토큰 응답이 있다면 Authorization 헤더로 추가
  let tokenHeader = {};
  try {
    const token = loginRes.json('accessToken') ?? loginRes.json('token');
    if (token) {
      tokenHeader = { Authorization: `Bearer ${token}` };
    }
  } catch (e) {
    // JSON 파싱 실패 시 토큰 없이 진행 (쿠키 세션 사용)
  }

  const commonParams = {
    headers: {
      Accept: 'application/json',
      ...tokenHeader,
    },
    cookies,
  };

  return commonParams;
}

export default function () {
  const commonParams = loginAndGetAuth();

  // 아이템 목록 조회
  const items = http.get(`${BASE_URL}/api/v1/items`, commonParams);
  check(items, { 'items status 200': (r) => r.status === 200 });

  let firstItemId;
  try {
    const parsed = items.json();
    const list = Array.isArray(parsed?.data) ? parsed.data : parsed; // 백엔드 응답 형태에 따라
    firstItemId = Array.isArray(list) && list.length > 0 ? list[0].id : undefined;
  } catch (e) {
    // JSON 파싱 실패 시 개별 아이템 테스트는 건너뜁니다.
  }

  // 카테고리 목록 조회
  const categories = http.get(`${BASE_URL}/api/v1/categories`, commonParams);
  check(categories, { 'categories status 200': (r) => r.status === 200 });

  // 첫 번째 아이템 상세 조회 (데이터가 있을 때만)
  if (firstItemId) {
    const itemDetail = http.get(`${BASE_URL}/api/v1/items/${firstItemId}`, commonParams);
    check(itemDetail, { 'item detail status 200': (r) => r.status === 200 });
  }

  // 회원가입 페이지 (permitAll)
//  const signup = http.get(`${BASE_URL}/api/v1/user/signup`);

  // actuator health 체크 (10회 중 1회만 호출하여 부하 분산)
//  if (__ITER % 10 === 0) {
//    const health = http.get(`${BASE_URL}/actuator/health`);
//    check(health, { 'health status 200': (r) => r.status === 200 });
//  }

  sleep(1);
}
