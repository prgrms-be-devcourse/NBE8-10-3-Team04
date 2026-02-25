package com.back.domain.user.user.repository

import com.back.domain.user.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    //Optional은 Service 변경하면서 수정할 예정
    fun findByLoginId(loginId: String): Optional<User>

    fun findByApiKey(apiKey: String): Optional<User>
}
