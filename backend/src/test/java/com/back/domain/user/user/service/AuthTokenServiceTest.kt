package com.back.domain.user.user.service

import com.back.domain.user.user.entity.User
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.Map

@SpringBootTest
@ActiveProfiles("test")
@Transactional
internal class AuthTokenServiceTest {
    @Autowired
    lateinit var authTokenService: AuthTokenService

    @Value("\${custom.jwt.secretKey}")
    lateinit var jwtSecretKey: String

    @Test
    @DisplayName("서비스 빈 로드 확인")
    fun service_should_be_loaded() {
        assertThat(authTokenService).isNotNull()
    }

    @Test
    @DisplayName("genAccessToken(): 생성된 토큰의 페이로드가 정상적으로 파싱")
    fun genAccessToken_verify_payload() {
        val user = User("testUser", "encodedPassword", "test@example.com")
        ReflectionTestUtils.setField(user, "_id", 123L)

        val accessToken = authTokenService.genAccessToken(user)
        val payload = authTokenService.payload(accessToken)

        assertThat(payload).isNotNull
        assertThat((payload!!["id"] as Number).toLong()).isEqualTo(123L)
        assertThat(payload["loginId"]).isEqualTo("testUser")
        assertThat(payload["email"]).isEqualTo("test@example.com")
        assertThat((payload["tokenVersion"] as Number).toLong()).isEqualTo(0L)
    }

    @Test
    @DisplayName("payload(): 모든 클레임(id, loginId, email, version)을 정확히 추출")
    fun payload_extracts_all_claims() {
        val expectedId = 456L
        val expectedLoginId = "payloadTestUser"

        val user = User(expectedLoginId, "password123", "payload@test.com")
        ReflectionTestUtils.setField(user, "_id", expectedId)

        repeat(3) { user.increaseTokenVersion() }

        val accessToken = authTokenService.genAccessToken(user)
        val payload = authTokenService.payload(accessToken)

        assertThat(payload).isNotNull()
        assertThat((payload!!["id"] as Number).toLong()).isEqualTo(expectedId)
        assertThat(payload["loginId"]).isEqualTo(expectedLoginId)
        assertThat((payload["tokenVersion"] as Number).toLong()).isEqualTo(3L)
        assertThat(payload).hasSize(4)
    }

    @Test
    @DisplayName("payload(): 형식이 잘못된 토큰은 null을 반환")
    fun payload_returns_null_for_malformed_token() {
        val invalidToken = "invalid.jwt.token"
        val payload = authTokenService.payload(invalidToken)
        assertThat(payload).isNull()
    }

    @Test
    @DisplayName("payload(): 만료된 토큰은 null을 반환")
    fun payload_returns_null_for_expired_token() {
        val keyBytes = jwtSecretKey.toByteArray(StandardCharsets.UTF_8)
        val secretKey = Keys.hmacShaKeyFor(keyBytes)

        val expiredToken = Jwts.builder()
            .claims(mapOf("id" to 999L))
            .issuedAt(Date(System.currentTimeMillis() - 2000))
            .expiration(Date(System.currentTimeMillis() - 1000))
            .signWith(secretKey)
            .compact()

        val payload = authTokenService.payload(expiredToken)
        assertThat(payload).isNull()
    }

    @Test
    @DisplayName("payload(): 서명이 다른(위조된) 토큰은 null을 반환")
    fun payload_returns_null_for_wrong_signature() {
        val wrongKeyStr = jwtSecretKey + "fake"
        val secretKey = Keys.hmacShaKeyFor(wrongKeyStr.toByteArray(StandardCharsets.UTF_8))

        val wrongSignedToken = Jwts.builder()
            .claims(mapOf("id" to 777L))
            .signWith(secretKey)
            .compact()

        val payload= authTokenService.payload(wrongSignedToken)
        assertThat(payload).isNull()
    }

    @Test
    @DisplayName("payload(): 숫자 타입(ID, Version)은 Long 값으로 정확히 비교")
    fun payload_handles_number_types_correctly() {
        val user = User("numberTestUser", "password", "number@test.com")
        ReflectionTestUtils.setField(user, "_id", 999999999L)

        repeat(5) { user.increaseTokenVersion() }

        val accessToken = authTokenService.genAccessToken(user)
        val payload = authTokenService.payload(accessToken)

        assertThat(payload).isNotNull

        val idValue = payload!!["id"]
        assertThat(idValue).isInstanceOf(Number::class.java)
        assertThat((idValue as Number).toLong()).isEqualTo(999999999L)

        val tokenVersionValue = payload["tokenVersion"]
        assertThat((tokenVersionValue as Number).toLong()).isEqualTo(5L)
    }

    @Test
    @DisplayName("통합 테스트: 서로 다른 사용자의 토큰은 서로 다른 값을 가져야 함")
    fun integration_multiple_users() {
        val user1 = User("u1", "p1", "u1@test.com")
        val user2 = User("u2", "p2", "u2@test.com")

        ReflectionTestUtils.setField(user1, "_id", 100L)
        ReflectionTestUtils.setField(user2, "_id", 200L)

        val token1 = authTokenService.genAccessToken(user1)
        val token2 = authTokenService.genAccessToken(user2)

        val p1 = authTokenService.payload(token1)
        val p2 = authTokenService.payload(token2)

        assertThat((p1!!["id"] as Number).toLong()).isEqualTo(100L)
        assertThat((p2!!["id"] as Number).toLong()).isEqualTo(200L)
        assertThat(token1).isNotEqualTo(token2)
    }

    @Test
    @DisplayName("payload(): 빈 문자열 토큰은 null을 반환")
    fun payload_returns_null_for_empty_string() {
        assertThat(authTokenService.payload("")).isNull()
    }
}