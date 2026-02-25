package com.back.domain.user.user.service;

import com.back.domain.user.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthTokenServiceTest {

    @Autowired
    private AuthTokenService authTokenService;

    @Value("${custom.jwt.secretKey}")
    private String jwtSecretKey;

    @Test
    @DisplayName("서비스 빈 로드 확인")
    void service_should_be_loaded() {
        assertThat(authTokenService).isNotNull();
    }

    @Test
    @DisplayName("genAccessToken(): 생성된 토큰의 페이로드가 정상적으로 파싱")
    void genAccessToken_verify_payload() {
        User user = new User("testUser", "encodedPassword", "test@example.com");
        ReflectionTestUtils.setField(user, "_id", 123L);

        String accessToken = authTokenService.genAccessToken(user);
        Map<String, Object> payload = authTokenService.payload(accessToken);

        assertThat(payload).isNotNull();
        assertThat(((Number) payload.get("id")).longValue()).isEqualTo(123L);
        assertThat(payload.get("loginId")).isEqualTo("testUser");
        assertThat(payload.get("email")).isEqualTo("test@example.com");
        assertThat(((Number) payload.get("tokenVersion")).longValue()).isEqualTo(0L);
    }

    @Test
    @DisplayName("payload(): 모든 클레임(id, loginId, email, version)을 정확히 추출")
    void payload_extracts_all_claims() {
        long expectedId = 456L;
        String expectedLoginId = "payloadTestUser";

        User user = new User(expectedLoginId, "password123", "payload@test.com");
        ReflectionTestUtils.setField(user, "_id", expectedId);

        user.increaseTokenVersion();
        user.increaseTokenVersion();
        user.increaseTokenVersion();

        String accessToken = authTokenService.genAccessToken(user);
        Map<String, Object> payload = authTokenService.payload(accessToken);

        assertThat(payload).isNotNull();
        assertThat(((Number) payload.get("id")).longValue()).isEqualTo(expectedId);
        assertThat(payload.get("loginId")).isEqualTo(expectedLoginId);
        assertThat(((Number) payload.get("tokenVersion")).longValue()).isEqualTo(3L);
        assertThat(payload).hasSize(4);
    }

    @Test
    @DisplayName("payload(): 형식이 잘못된 토큰은 null을 반환")
    void payload_returns_null_for_malformed_token() {
        String invalidToken = "invalid.jwt.token";
        Map<String, Object> payload = authTokenService.payload(invalidToken);
        assertThat(payload).isNull();
    }

    @Test
    @DisplayName("payload(): 만료된 토큰은 null을 반환")
    void payload_returns_null_for_expired_token() {
        byte[] keyBytes = jwtSecretKey.getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);

        String expiredToken = Jwts.builder()
                .claims(Map.of("id", 999L))
                .issuedAt(new Date(System.currentTimeMillis() - 2000))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(secretKey)
                .compact();

        Map<String, Object> payload = authTokenService.payload(expiredToken);
        assertThat(payload).isNull();
    }

    @Test
    @DisplayName("payload(): 서명이 다른(위조된) 토큰은 null을 반환")
    void payload_returns_null_for_wrong_signature() {
        String wrongKeyStr = jwtSecretKey + "fake";
        SecretKey secretKey = Keys.hmacShaKeyFor(wrongKeyStr.getBytes(StandardCharsets.UTF_8));

        String wrongSignedToken = Jwts.builder()
                .claims(Map.of("id", 777L))
                .signWith(secretKey)
                .compact();

        Map<String, Object> payload = authTokenService.payload(wrongSignedToken);
        assertThat(payload).isNull();
    }

    @Test
    @DisplayName("payload(): 숫자 타입(ID, Version)은 Long 값으로 정확히 비교")
    void payload_handles_number_types_correctly() {
        User user = new User("numberTestUser", "password", "number@test.com");
        ReflectionTestUtils.setField(user, "_id", 999999999L);

        for (int i = 0; i < 5; i++) user.increaseTokenVersion();

        String accessToken = authTokenService.genAccessToken(user);
        Map<String, Object> payload = authTokenService.payload(accessToken);

        assertThat(payload).isNotNull();

        Object idValue = payload.get("id");
        assertThat(idValue).isInstanceOf(Number.class);
        assertThat(((Number) idValue).longValue()).isEqualTo(999999999L);

        Object tokenVersionValue = payload.get("tokenVersion");
        assertThat(((Number) tokenVersionValue).longValue()).isEqualTo(5L);
    }

    @Test
    @DisplayName("통합 테스트: 서로 다른 사용자의 토큰은 서로 다른 값을 가져야 함")
    void integration_multiple_users() {
        User user1 = new User("u1", "p1", "u1@test.com");
        User user2 = new User("u2", "p2", "u2@test.com");
        ReflectionTestUtils.setField(user1, "_id", 100L);
        ReflectionTestUtils.setField(user2, "_id", 200L);

        String token1 = authTokenService.genAccessToken(user1);
        String token2 = authTokenService.genAccessToken(user2);

        Map<String, Object> p1 = authTokenService.payload(token1);
        Map<String, Object> p2 = authTokenService.payload(token2);

        assertThat(((Number) p1.get("id")).longValue()).isEqualTo(100L);
        assertThat(((Number) p2.get("id")).longValue()).isEqualTo(200L);
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("payload(): 빈 문자열 토큰은 null을 반환")
    void payload_returns_null_for_empty_string() {
        assertThat(authTokenService.payload("")).isNull();
    }
}