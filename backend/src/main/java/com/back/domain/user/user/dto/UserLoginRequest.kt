package com.back.domain.user.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserLoginRequest(
    @field:NotBlank
    @field:Size(min = 2, max = 30)
    val loginId: String,

    @field:NotBlank
    @field:Size(min = 2, max = 30)
    val password: String
)
