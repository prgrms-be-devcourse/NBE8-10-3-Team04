package com.back.global.rq

import com.back.domain.user.user.dto.UserDto
import com.back.domain.user.user.entity.User
import com.back.domain.user.user.service.UserService
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.security.SecurityUser
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class Rq(
    private val req: HttpServletRequest,
    private val resp: HttpServletResponse,
    private val userService: UserService
) {
    val actor: UserDto?
        get() {
            // 인증 정보를 가져옴
            val auth = SecurityContextHolder.getContext().authentication

            // 인증된 사용자인지 확인 후 SecurityUser로 안전하게 캐스팅
            val securityUser = auth?.takeIf { it.isAuthenticated }?.principal as? SecurityUser

            // 캐스팅에 성공했다면 UserDto로 변환
            return securityUser?.let {
                UserDto(
                    it.id,
                    it.loginId,
                    it.email
                )
            }
        }

    fun getHeader(name: String, defaultValue: String): String {
        return req.getHeader(name)?.takeIf { it.isNotBlank() } ?: defaultValue
    }

    // Java Stream 대신 코틀린 컬렉션의 firstOrNull 활용
    fun getCookieValue(name: String, defaultValue: String): String {
        return req.cookies
            ?.firstOrNull { it.name == name }
            ?.value
            ?.takeIf { it.isNotBlank() }
            ?: defaultValue
    }

    fun setCookie(name: String, value: String?) {
        val safeValue = value ?: ""

        // apply 스코프 함수를 사용하여 깔끔한 객체 초기화
        val cookie = Cookie(name, safeValue).apply {
            path = "/"
            isHttpOnly = true
            if (safeValue.isBlank()) {
                maxAge = 0
            }
        }

        resp.addCookie(cookie)
    }

    fun deleteCookie(name: String) {
        setCookie(name, null)
    }

    fun setHeader(name: String, value: String) {
        resp.setHeader(name, value)
    }

    val memberId: Long
        get() {
            val auth = SecurityContextHolder.getContext().authentication

            if (auth?.isAuthenticated != true) {
                throw ServiceException(ErrorCode.LOGIN_REQUIRED)
            }

            val principal = auth.principal
            if (principal !is SecurityUser) {
                throw ServiceException(ErrorCode.LOGIN_REQUIRED)
            }

            return principal.id
        }

    val member: User?
        get() = userService.findById(memberId)

    fun requireActor(): UserDto =
        actor ?: throw ServiceException(ErrorCode.LOGIN_REQUIRED)
}