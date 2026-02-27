package com.back.standard.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UtTest {
    @Value("${custom.jwt.secretKey}")
    private String SECRET;

    @Test
    @DisplayName("JWT 토큰 생성 테스트")
    void t1() {
        // 테스트용 데이터 맵 생성 (ID, 사용자명 포함) 및 만료 시간 설정
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1L);
        body.put("username", "testUser");
        int expireSeconds = 3600; // 1시간

        // Ut 유틸리티를 사용하여 JWT 문자열 생성
        String token = Ut.jwt.toString(SECRET, expireSeconds, body);

        // 토큰이 정상적으로 생성되었는지 확인 (null 체크, 비어있는지 체크)
        // JWT는 Header.Payload.Signature 3부분으로 구성되므로 점(.)으로 분리 시 3개여야 함
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("유효한 JWT 토큰 검증 - 성공")
    void t2() {
        // 검증할 정상적인 JWT 토큰 생성
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1L);
        body.put("username", "testUser");
        String token = Ut.jwt.toString(SECRET, 3600, body);

        // 올바른 시크릿 키로 토큰 유효성 검사 수행
        boolean isValid = Ut.jwt.isValid(SECRET, token);

        // 결과가 true인지 검증
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("잘못된 시크릿으로 JWT 토큰 검증 - 실패")
    void t3() {
        // 정상적인 시크릿 키로 토큰 생성
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1L);
        String token = Ut.jwt.toString(SECRET, 3600, body);

        // 검증 시 사용할 잘못된 시크릿 키 정의
        String wrongSecret = "wrongsecretwrongsecretwrongsecretwrongsecretwrongsecretwrongsecretwrongsecret";

        // 잘못된 키로 검증 시도
        boolean isValid = Ut.jwt.isValid(wrongSecret, token);

        // 서명이 일치하지 않으므로 false가 반환되어야 함
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("잘못된 형식의 JWT 토큰 검증 - 실패")
    void t4() {
        // JWT 형식이 아닌 임의의 문자열 준비
        String invalidToken = "invalid.token.format";

        // 유효성 검사 수행
        boolean isValid = Ut.jwt.isValid(SECRET, invalidToken);

        // 형식이 맞지 않으므로 false가 반환되어야 함
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("만료된 JWT 토큰 검증 - 실패")
    void t5() {
        // 만료 시간을 음수(-1)로 설정하여 이미 만료된 토큰 생성
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1L);
        String token = Ut.jwt.toString(SECRET, -1, body);

        // 유효성 검사 수행
        boolean isValid = Ut.jwt.isValid(SECRET, token);

        // 만료된 토큰이므로 false가 반환되어야 함
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("JWT 토큰에서 payload 추출 - 성공")
    void t6() {
        // 데이터가 담긴 정상 토큰 생성
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1L);
        body.put("username", "testUser");
        String token = Ut.jwt.toString(SECRET, 3600, body);

        // 토큰에서 Claims(Payload) 추출
        Claims claims = Ut.jwt.payload(SECRET, token);

        // Claims 객체가 존재하고, 내부에 저장했던 데이터가 정확히 일치하는지 검증
        assertThat(claims).isNotNull();
        assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
        assertThat(claims.get("username", String.class)).isEqualTo("testUser");
    }

    @Test
    @DisplayName("잘못된 시크릿으로 payload 추출 - null 반환")
    void t7() {
        // 정상 토큰 생성
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1L);
        String token = Ut.jwt.toString(SECRET, 3600, body);

        // 잘못된 시크릿 키 준비
        String wrongSecret = "wrongsecretwrongsecretwrongsecretwrongsecretwrongsecretwrongsecretwrongsecret";

        // 잘못된 키로 파싱 시도 (예외 발생 대신 null 반환 예상)
        Claims claims = Ut.jwt.payload(wrongSecret, token);

        // 파싱 실패로 null이어야 함
        assertThat(claims).isNull();
    }

    @Test
    @DisplayName("잘못된 형식의 토큰에서 payload 추출 - null 반환")
    void t8() {
        // 형식이 잘못된 토큰 문자열
        String invalidToken = "invalid.token.format";

        // 파싱 시도
        Claims claims = Ut.jwt.payload(SECRET, invalidToken);

        // 파싱 실패로 null이어야 함
        assertThat(claims).isNull();
    }

    @Test
    @DisplayName("객체를 JSON 문자열로 변환 - 성공")
    void t9() {
        // 변환할 Map 객체 생성
        Map<String, Object> obj = new HashMap<>();
        obj.put("name", "testUser");
        obj.put("age", 25);

        // 객체를 JSON 문자열로 변환
        String json = Ut.json.toString(obj);

        // 변환된 문자열이 null이 아니고, 키와 값이 포함되어 있는지 검증
        assertThat(json).isNotNull();
        assertThat(json).contains("\"name\"");
        assertThat(json).contains("\"testUser\"");
        assertThat(json).contains("\"age\"");
        assertThat(json).contains("25");
    }

    @Test
    @DisplayName("복잡한 객체를 JSON 문자열로 변환")
    void t10() {
        // 중첩된 구조의 Map 객체 생성
        Map<String, Object> nested = new HashMap<>();
        nested.put("nestedKey", "nestedValue");

        Map<String, Object> obj = new HashMap<>();
        obj.put("simple", "value");
        obj.put("nested", nested);

        // JSON 변환 수행
        String json = Ut.json.toString(obj);

        // 최상위 키와 중첩된 키/값이 모두 문자열에 포함되어 있는지 검증
        assertThat(json).isNotNull();
        assertThat(json).contains("\"simple\"");
        assertThat(json).contains("\"nested\"");
        assertThat(json).contains("\"nestedKey\"");
        assertThat(json).contains("\"nestedValue\"");
    }

    @Test
    @DisplayName("null 객체를 JSON 문자열로 변환")
    void t11() {
        // null을 전달하여 변환 시도
        String json = Ut.json.toString(null);

        // 결과가 문자열 "null"인지 검증
        assertThat(json).isNotNull();
        assertThat(json).isEqualTo("null");
    }

    @Test
    @DisplayName("JWT 토큰에 여러 타입의 클레임 포함")
    void t12() {
        // 문자열, 정수, 불리언 등 다양한 타입의 데이터 준비
        Map<String, Object> body = new HashMap<>();
        body.put("stringValue", "test");
        body.put("intValue", 123);
        body.put("boolValue", true);
        String token = Ut.jwt.toString(SECRET, 3600, body);

        // 토큰에서 Claims 추출
        Claims claims = Ut.jwt.payload(SECRET, token);

        // 각 데이터가 원래의 타입대로 잘 복원되었는지 검증
        assertThat(claims).isNotNull();
        assertThat(claims.get("stringValue", String.class)).isEqualTo("test");
        assertThat(claims.get("intValue", Integer.class)).isEqualTo(123);
        assertThat(claims.get("boolValue", Boolean.class)).isTrue();
    }

    @Test
    @DisplayName("JSON 직렬화 실패 시 예외 처리 테스트")
    void t13() {
        // 자기 자신을 참조하는 Map을 만들어 순환 참조(Circular Reference) 발생시킴
        Map<String, Object> map = new HashMap<>();
        map.put("self", map);

        String json = Ut.json.toString(map);

        // 예외가 발생하여 catch 블록에서 정의한 에러 JSON이 반환되는지 확인
        assertThat(json).contains("500-1");
        assertThat(json).contains("json serialize fail");
    }
}