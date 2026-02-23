package com.back.domain.user.user.dto

import com.back.domain.user.user.entity.User

data class UserUpdateResponse(
    val id: Long,
    val loginId: String,
    val email: String
) {
    companion object {
        /**
         * 수정된 User Entity를 Response DTO로 변환
         *
         * @param user 수정된 User 엔티티
         * @return UserUpdateResponse DTO
         */
        @JvmStatic
        fun from(user: User): UserUpdateResponse {
            return UserUpdateResponse(
                user.getId(),
                user.getLoginId(),
                user.getEmail()
            )
        }
    }
}