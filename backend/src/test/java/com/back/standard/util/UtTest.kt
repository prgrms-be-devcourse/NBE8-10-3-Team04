package com.back.standard.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
internal class UtTest {

    // lateinit var를 사용하여 불필요한 nullable 타입 및 !! 단언 연산자 제거
    @Value("\${custom.jwt.secretKey}")
    private lateinit var SECRET: String

    @Test
    @DisplayName("JWT 토큰 생성 테스트")
    fun t1() {
        // mapOf를 사용하여 간결하게 Map 생성
        val body = mapOf(
            "userId" to 1L,
            "username" to "testUser"
        )
        val expireSeconds = 3600 // 1시간

        val token = Ut.jwt.toString(SECRET, expireSeconds, body)

        assertThat(token).isNotNull()
        assertThat(token).isNotEmpty()

        // 코틀린의 split은 기본적으로 리터럴 문자열을 기준으로 분리하므로 정규식 불필요
        assertThat(token.split(".")).hasSize(3)
    }

    @Test
    @DisplayName("유효한 JWT 토큰 검증 - 성공")
    fun t2() {
        val body = mapOf(
            "userId" to 1L,
            "username" to "testUser"
        )
        val token = Ut.jwt.toString(SECRET, 3600, body)

        val isValid = Ut.jwt.isValid(SECRET, token)

        assertThat(isValid).isTrue()
    }

    @Test
    @DisplayName("잘못된 시크릿으로 JWT 토큰 검증 - 실패")
    fun t3() {
        val body = mapOf("userId" to 1L)
        val token = Ut.jwt.toString(SECRET, 3600, body)

        val wrongSecret = "wrongsecretwrongsecretwrongsecretwrongsecretwrongsecretwrongsecretwrongsecret"
        val isValid = Ut.jwt.isValid(wrongSecret, token)

        assertThat(isValid).isFalse()
    }

    @Test
    @DisplayName("잘못된 형식의 JWT 토큰 검증 - 실패")
    fun t4() {
        val invalidToken = "invalid.token.format"
        val isValid = Ut.jwt.isValid(SECRET, invalidToken)

        assertThat(isValid).isFalse()
    }

    @Test
    @DisplayName("만료된 JWT 토큰 검증 - 실패")
    fun t5() {
        val body = mapOf("userId" to 1L)
        val token = Ut.jwt.toString(SECRET, -1, body)

        val isValid = Ut.jwt.isValid(SECRET, token)

        assertThat(isValid).isFalse()
    }

    @Test
    @DisplayName("JWT 토큰에서 payload 추출 - 성공")
    fun t6() {
        val body = mapOf(
            "userId" to 1L,
            "username" to "testUser"
        )
        val token = Ut.jwt.toString(SECRET, 3600, body)

        val claims = Ut.jwt.payload(SECRET, token)

        // 불필요한 제네릭 타입(<String?, Any?>) 제거 및 타입 추론 활용
        assertThat(claims).isNotNull()

        // requireNotNull로 스마트 캐스트 유도하여 !! 없이 안전하게 참조
        requireNotNull(claims)
        assertThat(claims.get("userId", java.lang.Long::class.javaObjectType)).isEqualTo(1L)
        assertThat(claims.get("username", String::class.java)).isEqualTo("testUser")
    }

    @Test
    @DisplayName("잘못된 시크릿으로 payload 추출 - null 반환")
    fun t7() {
        val body = mapOf("userId" to 1L)
        val token = Ut.jwt.toString(SECRET, 3600, body)

        val wrongSecret = "wrongsecretwrongsecretwrongsecretwrongsecretwrongsecretwrongsecretwrongsecret"
        val claims = Ut.jwt.payload(wrongSecret, token)

        assertThat(claims).isNull()
    }

    @Test
    @DisplayName("잘못된 형식의 토큰에서 payload 추출 - null 반환")
    fun t8() {
        val invalidToken = "invalid.token.format"
        val claims = Ut.jwt.payload(SECRET, invalidToken)

        assertThat(claims).isNull()
    }

    @Test
    @DisplayName("객체를 JSON 문자열로 변환 - 성공")
    fun t9() {
        val obj = mapOf(
            "name" to "testUser",
            "age" to 25
        )

        val json = Ut.json.toString(obj)

        assertThat(json).isNotNull()
        // 여러 번 호출할 필요 없이 contains 안에 가변 인자로 묶어서 검증
        assertThat(json).contains("\"name\"", "\"testUser\"", "\"age\"", "25")
    }

    @Test
    @DisplayName("복잡한 객체를 JSON 문자열로 변환")
    fun t10() {
        val nested = mapOf("nestedKey" to "nestedValue")
        val obj = mapOf(
            "simple" to "value",
            "nested" to nested
        )

        val json = Ut.json.toString(obj)

        assertThat(json).isNotNull()
        assertThat(json).contains("\"simple\"", "\"nested\"", "\"nestedKey\"", "\"nestedValue\"")
    }

    @Test
    @DisplayName("null 객체를 JSON 문자열로 변환")
    fun t11() {
        val json = Ut.json.toString(null)

        assertThat(json).isNotNull().isEqualTo("null")
    }

    @Test
    @DisplayName("JWT 토큰에 여러 타입의 클레임 포함")
    fun t12() {
        val body = mapOf(
            "stringValue" to "test",
            "intValue" to 123,
            "boolValue" to true
        )
        val token = Ut.jwt.toString(SECRET, 3600, body)

        val claims = Ut.jwt.payload(SECRET, token)

        requireNotNull(claims)
        assertThat(claims.get("stringValue", String::class.java)).isEqualTo("test")
        // 원시 타입 박싱 처리를 위해 javaObjectType 사용
        assertThat(claims.get("intValue", Int::class.javaObjectType)).isEqualTo(123)
        assertThat(claims.get("boolValue", Boolean::class.javaObjectType)).isTrue()
    }

    @Test
    @DisplayName("JSON 직렬화 실패 시 예외 처리 테스트")
    fun t13() {
        // 자기 자신을 참조해야 하므로 mutableMapOf 사용
        val map = mutableMapOf<String, Any>()
        map["self"] = map

        val json = Ut.json.toString(map)

        assertThat(json).contains("500-1", "json serialize fail")
    }
}