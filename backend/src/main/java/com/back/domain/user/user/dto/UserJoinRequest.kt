package com.back.domain.user.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

//회원가입
data class UserJoinRequest(
    @field:NotBlank
    @field:Size(min = 2, max = 30)
    val loginId: String,

    @field:NotBlank
    @field:Size(min = 2, max = 30)
    val password: String,

    @field:Pattern(
        // 정규식으로 . 뒤에 2글자 이상의 도메인이 오도록 규칙설정
        regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
        message = "올바른 이메일 형식을 입력해주세요."
    )
    @field:NotBlank
    @field:Size(min = 2, max = 30)
    val email: String
)
