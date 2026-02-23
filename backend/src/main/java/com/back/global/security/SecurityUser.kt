package com.back.global.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

class SecurityUser(
    val id: Long,
    loginId: String,
    password: String?,
    val email: String,
    authorities: Collection<GrantedAuthority>
) : User(loginId, password ?: "", authorities) {

    fun getLoginId(): String = username
}