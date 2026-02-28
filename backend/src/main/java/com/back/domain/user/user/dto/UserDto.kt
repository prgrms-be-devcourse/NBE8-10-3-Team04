package com.back.domain.user.user.dto

import com.back.domain.user.user.entity.User

data class UserDto(
    val id: Long,
    val loginId: String,
    val email: String
) {
    companion object {
        /**
         * Entity -> DTO 변환을 위한 정적 팩토리 메서드
         *
         * @param user 변환할 User 엔티티
         * @return UserDto
         */
        @JvmStatic
        fun from(user: User): UserDto =
            UserDto(
                user.persistedId,
                user.loginId,
                user.email
            )

        /**
         * 여러 Entity를 한번에 변환
         *
         * @param users 변환할 User 엔티티 리스트
         * @return UserDto 리스트
         */
        @JvmStatic
        fun fromList(users: List<User>): List<UserDto> =
            users.map { from(it) }

    }
}