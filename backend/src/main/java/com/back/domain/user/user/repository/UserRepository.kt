package com.back.domain.user.user.repository

import com.back.domain.user.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByLoginId(loginId: String): User?

    fun findByApiKey(apiKey: String): User?
}
