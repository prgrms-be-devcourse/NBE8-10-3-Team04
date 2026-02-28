package com.back.global.security;

import com.back.domain.user.user.entity.User
import com.back.domain.user.user.repository.UserRepository
import com.back.domain.user.user.service.UserService
import com.back.global.exception.*
import com.back.global.rq.Rq
import com.back.standard.util.Ut
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter


@Component
class CustomAuthenticationFilter(
    private val userRepository: UserRepository,
    private val rq: Rq,
    private val userService: UserService
) : OncePerRequestFilter() {

    @Value("\${custom.jwt.secretKey}")
    private lateinit var jwtSecret: String

    companion object {
        private val PUBLIC_APIS = setOf(
            "/api/v1/user/login",
            "/api/v1/user/signup",
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        runCatching {
            work(request, response, filterChain)
        }.onFailure { e ->
            if (e is ServiceException) {
                val rsData = e.rsData
                response.apply {
                    contentType = "application/json;charset=UTF-8"
                    status = rsData.statusCode
                    writer.write(Ut.json.toString(rsData))
                }
            } else {
                throw e
            }
        }
    }

    private fun work(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        // 1) API 요청이 아닌 경우 패스
        if (!request.requestURI.startsWith("/api/")) {
            filterChain.doFilter(request, response)
            return
        }

        // 2) 인증/인가가 필요없는 API 요청 패스
        if (request.requestURI in PUBLIC_APIS) {
            filterChain.doFilter(request, response)
            return
        }

        // 3) apiKey, accessToken 추출
        val apiKey: String
        val accessToken: String
        val headerAuthorization: String = rq.getHeader("Authorization", "")

        // 3-1) Authorization 헤더일 때
        if (!headerAuthorization.isBlank()) {
            if (!headerAuthorization.startsWith("Bearer ")) {
                throw ServiceException(ErrorCode.INVALID_AUTH_HEADER)
            }
            val headerAuthorizationBits = headerAuthorization.split(" ", limit = 3)

            apiKey = headerAuthorizationBits[1]
            accessToken = if (headerAuthorizationBits.size == 3) headerAuthorizationBits[2] else ""
        } else {
            // 3-2) 쿠키일 때
            apiKey = rq.getCookieValue("apiKey", "")
            accessToken = rq.getCookieValue("accessToken", "")
        }

        // apikey, accessToken이 모두 없으면 통과 (익명 요청)
        val isApiKeyExists = apiKey.isNotBlank()
        val isAccessTokenExists = accessToken.isNotBlank()
        if (!isApiKeyExists && !isAccessTokenExists) {
            filterChain.doFilter(request, response)
            return
        }

        // 4) accessToken, apiKey 둘 중 하나라도 있다면 인증/인가 수행
        var user: User? = null

        // 4-1) accessToken 우선 검증 및 파싱
        var isAccessTokenValid = false
        if (isAccessTokenExists) {
            Ut.jwt.payload(jwtSecret, accessToken)?.let { claims ->

                // 5) 토큰에서 회원ID 추출
                val id = (claims["id"] as? Number)?.toLong()
                val loginId = claims["loginId"] as? String
                val tokenVersion = (claims["tokenVersion"] as? Number)?.toLong()

                // 클레임 유효성 검사
                if (id == null || loginId == null) {
                    throw ServiceException(ErrorCode.INVALID_TOKEN_CLAIM)
                }

                // DB에서 실제 회원 조회
                user = userRepository.findById(id)
                    .orElseThrow { ServiceException(ErrorCode.USER_NOT_FOUND) }


                // tokenVersion 검증 - DB의 버전과 토큰의 버전이 다르면 토큰 무효화
                if (tokenVersion == null || tokenVersion != user.tokenVersion) {
                    throw ServiceException(ErrorCode.TOKEN_EXPIRED)
                }

                isAccessTokenValid = true
            }
        }

        // 4-2) accessToken이 없으면 apiKey 탐색
        if (user == null) {
            user = userService.findByApiKey(apiKey)
                ?: throw ServiceException(ErrorCode.INVALID_API_KEY)
        }

        // accessToken이 만료되었거나 유효하지 않다면 apiKey를 통해서 재발급
        if (isAccessTokenExists && !isAccessTokenValid) {
            val u = user ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)
            val userAccessToken = userService.genAccessToken(u)

            rq.setCookie("accessToken", userAccessToken)
            rq.setHeader("Authorization", userAccessToken)
        }

        // 7) accessToken이 만료되었는지 확인 (선택적 - 만료 시간 체크)
        // 현재는 토큰이 유효하면 통과, 만료되면 위에서 이미 null 반환됨

        // 8) accessToken이 유효하지만 만료 시간이 가까우면 새로 발급 (선택적)
        // 필요시 토큰 만료 시간을 체크하여 재발급할 수 있음
        // 현재는 토큰이 유효하면 그대로 사용

        // 9) SecurityContext에 인증 정보 주입
        val securityUser: UserDetails = SecurityUser(
            user.id!!,
            user.loginId,
            "",
            user.email ?: "",
            emptyList()
        )

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                securityUser,
                securityUser.password,
                securityUser.authorities
            )

        // 10) 다음 필터로 넘김
        filterChain.doFilter(request, response)
    }
}