package com.back.domain.user.user.service

import com.back.domain.user.user.entity.User
import com.back.domain.user.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.s3.S3ImageService
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService(
    private val authTokenService: AuthTokenService,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val s3ImageService: S3ImageService,
) {

    @Transactional(readOnly = true)
    fun count(): Long = userRepository.count()

    @Transactional
    fun join(loginId: String, password: String, email: String): User {
        if (userRepository.findByLoginId(loginId) != null) {
            throw ServiceException(ErrorCode.DUPLICATE_LOGIN_ID)
        }

        val encodedPassword = passwordEncoder.encode(password)!! //패스워드 암호화 추가  !!로 null 안됨 강제성 부여
        val user = User(loginId, encodedPassword, email)
        return userRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun findByLoginId(loginId: String) = userRepository.findByLoginId(loginId)


    @Transactional
    fun deleteById(id: Long) {
        val user = userRepository.findById(id)
            .orElseThrow{ ServiceException(ErrorCode.USER_NOT_FOUND) }

        val imageUrls = user.items
            .mapNotNull { it.imgUrl?.takeIf(String::isNotBlank) }

        // API 요청 횟수 감소를 위해 S3 다중 삭제 사용
        if (imageUrls.isNotEmpty()) {
            s3ImageService.deleteMultiple(imageUrls)
        }
        userRepository.deleteById(id)
    }

    @Transactional
    fun checkPassword(user: User, password: String) {
        if (!passwordEncoder.matches(password, user.password)) {
            throw ServiceException(ErrorCode.INVALID_PASSWORD)
        }
    }

    @Transactional(readOnly = true)
    fun findByApiKey(apiKey: String) = userRepository.findByApiKey(apiKey)

    @Transactional
    fun genAccessToken(user: User): String = authTokenService.genAccessToken(user)

    @Transactional(readOnly = true)
    fun findById(id: Long): User? =
        userRepository.findById(id).orElse(null)

    /**
     * 프로필(이메일) 수정
     * PATCH /api/v1/user/me
     * 이메일 변경 시 기존 토큰 무효화 (tokenVersion 증가 + apiKey 재생성)
     */
    @Transactional
    fun updateProfile(
        id: Long,
        email: String
    ): User {
        val user = userRepository.findById(id)
            .orElseThrow { ServiceException(ErrorCode.USER_NOT_FOUND) }

        user.modifyUser(email, user.password)

        // 기존 토큰 무효화: tokenVersion 증가 + apiKey 재생성
        user.increaseTokenVersion()
        user.modifyApiKey(UUID.randomUUID().toString())

        return user
    }

    /**
     * 비밀번호 변경
     * PATCH /api/v1/user/me/password
     * 현재 비밀번호 검증 후 새 비밀번호로 변경
     * 비밀번호 변경 시 기존 모든 토큰 무효화 (tokenVersion 증가 + apiKey 재생성)
     */
    @Transactional
    fun changePassword(
        id: Long,
        currentPassword: String,
        newPassword: String
    ): User {
        val user = userRepository.findById(id)
            .orElseThrow { ServiceException(ErrorCode.USER_NOT_FOUND) }

        if (!passwordEncoder.matches(currentPassword, user.password)) {
            throw ServiceException(ErrorCode.PASSWORD_MISMATCH)
        }

        if (passwordEncoder.matches(newPassword, user.password)) {
            throw ServiceException(ErrorCode.SAME_PASSWORD)
        }

        val encodedPassword = passwordEncoder.encode(newPassword)!!
        user.modifyUser(user.email, encodedPassword)

        // 기존 모든 토큰 무효화: tokenVersion 증가 + apiKey 재생성
        user.increaseTokenVersion()
        user.modifyApiKey(UUID.randomUUID().toString())

        return user
    }
}
