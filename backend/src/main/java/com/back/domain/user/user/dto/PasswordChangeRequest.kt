package com.back.domain.user.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 비밀번호 변경 요청
 * PATCH /api/v1/user/me/password
 */
data class PasswordChangeRequest(
    @field:NotBlank(message = "현재 비밀번호를 입력해주세요.")
    @field:Size(min = 2, max = 30)
    val currentPassword: String,

    @field:NotBlank(message = "새 비밀번호를 입력해주세요.")
    @field:Size(min = 2, max = 20)
    val newPassword: String
)
