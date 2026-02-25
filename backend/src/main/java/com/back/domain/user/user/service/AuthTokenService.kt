package com.back.domain.user.user.service

import com.back.domain.user.user.entity.User
import com.back.standard.util.Ut
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AuthTokenService(
    @Value("\${custom.jwt.secretKey}")
    private val jwtSecretKey: String,

    @Value("\${custom.accessToken.expirationSeconds}")
    private val accessTokenExpirationSeconds: Int
) {
    fun genAccessToken(user: User): String {
        return Ut.jwt.toString(
            jwtSecretKey,
            accessTokenExpirationSeconds,
            mapOf(
                "id" to user.id,
                "loginId" to user.loginId,
                "email" to user.email,
                "tokenVersion" to user.tokenVersion
            )
        )
    }

    fun payload(accessToken: String): Map<String, Any>? {
        val parsedPayload = Ut.jwt.payload(jwtSecretKey, accessToken)
            ?: return null

        return mapOf(
            "id" to (parsedPayload["id"] as Number).toLong(),
            "loginId" to parsedPayload["loginId"] as String,
            "email" to parsedPayload["email"] as String,
            "tokenVersion" to (parsedPayload["tokenVersion"] as Number).toLong()
        )
    }
}