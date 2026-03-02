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

// 환경 변수 누락 방지 로직
if (!BASE_URL || !USER_ID || !USER_PW) {
  throw new Error('환경변수(BASE_URL, USER_ID, USER_PW)가 설정되지 않았습니다.');
}

// 1. 마스터(setup)가 1번만 로그인해서 쿠키를 발급받음
export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/api/v1/user/login`,
    JSON.stringify({ loginId: USER_ID, password: USER_PW }),
    {
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    }
  );

  const loginOk = check(loginRes, { 'setup login success': (r) => r.status === 200 });
  if (!loginOk) fail(`초기 로그인 실패 (status: ${loginRes.status})`);

  // 쿠키를 깔끔한 자바스크립트 객체로 변환
  const cookiesObj = {};
  if (loginRes.cookies) {
    for (const [name, values] of Object.entries(loginRes.cookies)) {
      if (values.length > 0) cookiesObj[name] = values[0].value;
    }
  }

  // 💡 문제의 Authorization 토큰은 제외하고 쿠키만 300명에게 전달!
  return { cookies: cookiesObj };
}

// 300명의 가상 사용자가 반복하는 구간
export default function (data) {

  const commonParams = {
    headers: { Accept: 'application/json' },
    cookies: data.cookies, // 모든 VU가 셋업에서 받은 쿠키를 장착
  };

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
