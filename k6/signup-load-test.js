import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 100 },  // 10초 동안 100명의 유저로 증가
    { duration: '30s', target: 200 },  // 30초 동안 200명의 유저 유지하며 집중 부하
    { duration: '10s', target: 0 },    // 10초 동안 0명으로 감소
  ],
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // 항상 동일한 ID로 가입 시도 (DB 중복 검증 로직을 집중적으로 타격)
  const payload = JSON.stringify({
    loginId: 'duplicate_test_id',
    password: 'Password123!',
    email: 'test@example.com'
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
  };

  const res = http.post(`${BASE_URL}/api/v1/user/signup`, payload, params);

  // 이미 존재하는 아이디이므로 400번대 에러(Duplicate ID)가 정상입니다.
  check(res, {
      'is duplicate error (status 409)': (r) => r.status === 409,
  });

  // 매우 짧은 대기 시간으로 DB에 부하를 극대화
  sleep(0.1);
}