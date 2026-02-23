package com.back.domain.user.user.dto

import com.back.domain.user.user.entity.User

data class UserLoginResponse(
    val user: UserDto,
    val apiKey: String,
    val accessToken: String
) {
    companion object {
        /**
         * 로그인 성공 시 User Entity와 토큰들을 Response DTO로 변환
         *
         * @param user 로그인한 사용자
         * @param apiKey API 키
         * @param accessToken 액세스 토큰
         * @return UserLoginResponse DTO
         */
        @JvmStatic
        fun of(user: User, apiKey: String, accessToken: String): UserLoginResponse {
            return UserLoginResponse(
                user = UserDto.from(user),
                apiKey = apiKey,
                accessToken = accessToken
            )
        }
    }
}