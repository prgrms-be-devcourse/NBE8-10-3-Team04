package com.back.standard.util

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.Date

/**
 * 프로젝트 전반에 사용되는 공통 유틸리티 객체 (싱글톤)
 * Kotlin의 object 키워드를 사용하여 인스턴스화 없이 바로 접근 가능
 */
object Ut {
    // JWT 검증, 파싱, 생성 관련 유틸리티
    object jwt {
        /**
         * JWT 토큰의 유효성 검증
         * @param secret JWT 서명에 사용된 시크릿 키
         * @param jwtStr 검증할 JWT 토큰 문자열
         * @return 유효한 토큰이면 true, 아니면 false
         */
        @JvmStatic
        fun isValid(secret: String, jwtStr: String?): Boolean {
            // 토큰이 비어있거나 null이면 false
            if (jwtStr.isNullOrBlank()) return false

            // runCatching을 통해 try-catch 대체하여 예외 처리
            return runCatching {
                val secretKey = Keys.hmacShaKeyFor(secret.toByteArray())
                Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(jwtStr)
            }.isSuccess // 예외가 발생하지 않으면 true
        }

        /**
         * JWT 토큰에서 Payload 데이터 추출
         * @param secret JWT 서명에 사용된 시크릿키
         * @param jwtStr 해석할 JWT 토큰 문자열
         * @return 추출된 Claims 객체 (검증 실패 시 null)
         */
        @JvmStatic
        fun payload(secret: String, jwtStr: String?): Claims? {
            // 토큰이 비어있으면 null
            if (jwtStr.isNullOrBlank()) return null

            return runCatching {
                val secretKey = Keys.hmacShaKeyFor(secret.toByteArray())
                Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(jwtStr)
                    .payload // getPayload() 대신 프로퍼티 접근
            }.getOrNull() // 파싱 성공하면 Claims, 실패하면 null
        }

        /**
         * 새로운 JWT 토큰 발급
         * @param secret 서명에 사용할 시크릿 키
         * @param expireSeconds 토큰 만료 시간 (초 단위)
         * @param body 토큰 Payload에 담을 데이터 맵
         * @return 생성된 JWT 토큰 문자열
         */
        @JvmStatic
        fun toString(secret: String, expireSeconds: Int, body: Map<String, Any?>): String {
            val claimsBuilder = Jwts.claims()

            // Map의 데이터를 구조 분해 할당(key, value)을 통해 Claims에 추가
            body.forEach { (key, value) -> claimsBuilder.add(key, value) }

            val issuedAt = Date()
            // 현재 시간 + (초 단위 만료 시간 * 1000)으로 밀리초 단위 만료 시간 계산
            val expiration = Date(issuedAt.time + expireSeconds * 1000L) // getTime() 대신 time
            val secretKey = Keys.hmacShaKeyFor(secret.toByteArray())

            // JWT 빌더를 사용하여 토큰 조립 및 서명
            return Jwts.builder()
                .claims(claimsBuilder.build()) // 클레임 세팅
                .issuedAt(issuedAt) // 발행 일자
                .expiration(expiration) // 만료 일자
                .signWith(secretKey) // 시크릿 키로 서명
                .compact() // 최종 문자열로 직렬화
        }
    }

    // JSON 직렬화 관련 유틸리티
    object json {
        // Jackson ObjectMapper 인스턴스 (재사용)
        private val objectMapper = ObjectMapper()

        /**
         * 객체를 JSON 형태의 문자열로 변환
         * @param obj JSON으로 직렬화할 객체
         * @return 변환된 JSON 문자열 (실패 시 에러 메시지를 담은 JSON 문자열 반환)
         */
        @JvmStatic
        fun toString(obj: Any?): String {
            return runCatching {
                objectMapper.writeValueAsString(obj)
            }.getOrDefault("""{"resultCode":"500-1","msg":"json serialize fail"}""")
            // 성공하면 JSON 문자열 반환, 실패 시 작성된 이스케이프 없는 Raw String 에러 반환
        }
    }
}