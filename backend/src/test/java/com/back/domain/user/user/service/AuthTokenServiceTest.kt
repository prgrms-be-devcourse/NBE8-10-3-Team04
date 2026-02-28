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
import java.util.Date

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
        val payload = requireNotNull(
            payloadOf(
                user = User("testUser", "encodedPassword", "test@example.com"),
                id = 123L
            )
        )

        payload.assertClaims(
            id = 123L,
            loginId = "testUser",
            email = "test@example.com",
            tokenVersion = 0L
        )
    }

    @Test
    @DisplayName("payload(): 모든 클레임(id, loginId, email, version)을 정확히 추출")
    fun payload_extracts_all_claims() {
        val user = User("payloadTestUser", "password123", "payload@test.com").apply {
            ReflectionTestUtils.setField(this, "id", 456L)
            repeat(3) { increaseTokenVersion() }
        }

        val payload = requireNotNull(
            authTokenService.payload(authTokenService.genAccessToken(user))
        )

        payload.assertClaims(
            id = 456L,
            loginId = "payloadTestUser",
            email = "payload@test.com",
            tokenVersion = 3L
        )
        assertThat(payload).hasSize(4)
    }

    @Test
    @DisplayName("payload(): 형식이 잘못된 토큰은 null을 반환")
    fun payload_returns_null_for_malformed_token() {
        assertThat(authTokenService.payload("invalid.jwt.token")).isNull()
    }

    @Test
    @DisplayName("payload(): 만료된 토큰은 null을 반환")
    fun payload_returns_null_for_expired_token() {
        val secretKey = Keys.hmacShaKeyFor(jwtSecretKey.toByteArray(StandardCharsets.UTF_8))

        val expiredToken = Jwts.builder()
            .claims(mapOf("id" to 999L))
            .issuedAt(Date(System.currentTimeMillis() - 2000))
            .expiration(Date(System.currentTimeMillis() - 1000))
            .signWith(secretKey)
            .compact()

        assertThat(authTokenService.payload(expiredToken)).isNull()
    }

    @Test
    @DisplayName("payload(): 서명이 다른(위조된) 토큰은 null을 반환")
    fun payload_returns_null_for_wrong_signature() {
        val wrongKey = (jwtSecretKey + "fake").toByteArray(StandardCharsets.UTF_8)
        val secretKey = Keys.hmacShaKeyFor(wrongKey)

        val wrongSignedToken = Jwts.builder()
            .claims(mapOf("id" to 777L))
            .signWith(secretKey)
            .compact()

        assertThat(authTokenService.payload(wrongSignedToken)).isNull()
    }

    @Test
    @DisplayName("payload(): 숫자 타입(ID, Version)은 Long 값으로 정확히 비교")
    fun payload_handles_number_types_correctly() {
        val user = User("numberTestUser", "password", "number@test.com").apply {
            ReflectionTestUtils.setField(this, "id", 999_999_999L)
            repeat(5) { increaseTokenVersion() }
        }

        val payload = requireNotNull(
            authTokenService.payload(authTokenService.genAccessToken(user))
        )

        assertThat(payload.long("id")).isEqualTo(999_999_999L)
        assertThat(payload.long("tokenVersion")).isEqualTo(5L)
    }

    @Test
    @DisplayName("통합 테스트: 서로 다른 사용자의 토큰은 서로 다른 값을 가져야 함")
    fun integration_multiple_users() {
        val p1 = requireNotNull(payloadOf(User("u1", "p1", "u1@test.com"), id = 100L))
        val p2 = requireNotNull(payloadOf(User("u2", "p2", "u2@test.com"), id = 200L))

        val token1 = authTokenService.genAccessToken(User("u1", "p1", "u1@test.com").withId(100L))
        val token2 = authTokenService.genAccessToken(User("u2", "p2", "u2@test.com").withId(200L))

        assertThat(p1.long("id")).isEqualTo(100L)
        assertThat(p2.long("id")).isEqualTo(200L)
        assertThat(token1).isNotEqualTo(token2)
    }

    @Test
    @DisplayName("payload(): 빈 문자열 토큰은 null을 반환")
    fun payload_returns_null_for_empty_string() {
        assertThat(authTokenService.payload("")).isNull()
    }

    // ---------- helpers ----------

    private fun payloadOf(user: User, id: Long): Map<String, Any?>? {
        user.withId(id)
        val token = authTokenService.genAccessToken(user)
        return authTokenService.payload(token)
    }

    private fun User.withId(id: Long): User = apply {
        ReflectionTestUtils.setField(this, "id", id)
    }

    //Long으로 전환
    private fun Map<String, Any?>.long(key: String): Long =
        (this[key] as Number).toLong()

    private fun Map<String, Any?>.assertClaims(
        id: Long,
        loginId: String,
        email: String,
        tokenVersion: Long,
    ) {
        assertThat(long("id")).isEqualTo(id)
        assertThat(this["loginId"]).isEqualTo(loginId)
        assertThat(this["email"]).isEqualTo(email)
        assertThat(long("tokenVersion")).isEqualTo(tokenVersion)
    }
}