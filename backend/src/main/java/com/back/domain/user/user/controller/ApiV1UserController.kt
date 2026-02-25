package com.back.domain.user.user.controller

import com.back.domain.user.user.dto.*
import com.back.domain.user.user.service.UserService
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/user")
@Tag(name = "ApiV1UserController", description = "API 회원 컨트롤러") //Swagger 문서 태그용
class ApiV1UserController(
    private val userService: UserService,
    private val rq: Rq
) {

    @PostMapping("/signup")
    @Transactional
    @Operation(summary = "회원가입")
    fun join(@Valid @RequestBody request: UserJoinRequest): RsData<UserDto> {
        // BindingResult 제거 - GlobalExceptionHandler가 자동으로 처리
        // 유효성 검증 실패 시 MethodArgumentNotValidException 발생 → 핸들러가 처리

        // 성공 시 로직 실행
        val user = userService.join(
            request.loginId,
            request.password,
            request.email
        )

        return RsData(
            "201-1",
            "${user.loginId}님 환영합니다. 회원가입이 완료되었습니다.",
            UserDto.from(user)
        )
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    @Operation(summary = "로그인")
    fun login(
        @Valid @RequestBody reqBody: UserLoginRequest
    ): RsData<UserLoginResponse> {
        val user = userService.findByLoginId(reqBody.loginId)
            ?: throw ServiceException(ErrorCode.INVALID_LOGIN_ID)

        userService.checkPassword(user, reqBody.password)

        val accessToken = userService.genAccessToken(user)

        rq.setCookie("apiKey", user.apiKey)
        rq.setCookie("accessToken", accessToken)

        return RsData(
            "200-1",
            "${user.loginId}님 환영합니다.",
            UserLoginResponse.of(user, user.apiKey, accessToken)
        )
    }

    @DeleteMapping("/me")
    @Operation(summary = "탈퇴")
    fun deleteMe(): RsData<UserDto> {
        val actor = rq.actor
            ?: throw ServiceException(ErrorCode.LOGIN_REQUIRED)

        userService.deleteById(actor.id)
        rq.setCookie("accessToken", "")

        return RsData(
            "200-1",
            "${actor.loginId}님의 정보입니다.",
            actor
        )
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회")
    fun me(): RsData<UserDto> {
        val actor = rq.actor
            ?: throw ServiceException(ErrorCode.LOGIN_REQUIRED)

        // UserDto는 이미 필요한 정보를 포함하고 있으므로 그대로 반환
        return RsData(
            "200-1",
            "${actor.loginId}님의 정보입니다.",
            actor
        )
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃")
    fun logout(): RsData<Void> {
        rq.deleteCookie("apiKey")
        rq.deleteCookie("accessToken")

        return RsData(
            "200-1",
            "로그아웃 되었습니다."
        )
    }

    @PatchMapping("/me")
    @Operation(summary = "프로필(이메일) 수정")
    fun updateProfile(
        @Valid @RequestBody request: UserProfileUpdateRequest
    ): RsData<UserUpdateResponse> {
        val actor = rq.actor
            ?: throw ServiceException(ErrorCode.LOGIN_REQUIRED)

        val updatedUser = userService.updateProfile(actor.id, request.email)

        // 새 토큰 발급 및 쿠키 갱신 (기존 토큰은 무효화됨)
        val newAccessToken = userService.genAccessToken(updatedUser)
        rq.setCookie("accessToken", newAccessToken)
        rq.setCookie("apiKey", updatedUser.apiKey)

        return RsData(
            "200-2",
            "회원정보가 수정되었습니다.",
            UserUpdateResponse.from(updatedUser)
        )
    }

    @PatchMapping("/me/password")
    @Operation(summary = "비밀번호 변경")
    fun changePassword(
        @Valid @RequestBody request: PasswordChangeRequest
    ): RsData<UserUpdateResponse> {
        val actor = rq.actor
            ?: throw ServiceException(ErrorCode.LOGIN_REQUIRED)

        val updatedUser = userService.changePassword(
            actor.id,
            request.currentPassword,
            request.newPassword
        )

        // 새 토큰 발급 및 쿠키 갱신 (기존 모든 토큰은 무효화됨)
        val newAccessToken = userService.genAccessToken(updatedUser)
        rq.setCookie("accessToken", newAccessToken)
        rq.setCookie("apiKey", updatedUser.apiKey)

        return RsData(
            "200-2",
            "비밀번호가 변경되었습니다.",
            UserUpdateResponse.from(updatedUser)
        )
    }

    @PostMapping("/me/verify-password")
    @Operation(summary = "비밀번호 확인")
    fun verifyPassword(
        @Valid  @RequestBody request: PasswordVerifyRequest
    ): RsData<Void> {
        val actor = rq.actor
            ?: throw ServiceException(ErrorCode.LOGIN_REQUIRED)

        val user = userService.findById(actor.id)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)

        userService.checkPassword(user, request.password)

        return RsData(
            "200-1",
            "비밀번호가 확인되었습니다."
        )
    }
}
