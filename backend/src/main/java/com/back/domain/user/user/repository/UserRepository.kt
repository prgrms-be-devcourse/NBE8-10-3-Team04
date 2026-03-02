package com.back.domain.user.user.repository

import com.back.domain.user.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByLoginId(loginId: String): User?

    // 존재 여부만 확인하는 메서드 추가
    fun existsByLoginId(loginId: String): Boolean

    fun findByApiKey(apiKey: String): User?
}
