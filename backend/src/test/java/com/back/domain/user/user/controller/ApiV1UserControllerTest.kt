package com.back.domain.user.user.controller

import com.back.domain.user.user.service.UserService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class ApiV1UserControllerTest {
    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var mvc: MockMvc

    @Test
    @DisplayName("회원가입")
    fun t1() {
        val resultActions = mvc
            .perform(
                post("/api/v1/user/signup")
                    .with(SecurityMockMvcRequestPostProcessors.csrf()) //
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "loginId": "usernew",
                                            "password": "1234",
                                            "email": "test@test.com"
                                        }
                                        
                                        """.trimIndent()
                    ) //
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("join"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("201-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").exists())

        resultActions
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.loginId").value("usernew"))
    }

    @Test
    @DisplayName("회원가입: 유효성검증실패-아이디누락")
    fun t1_2() {
        // BindingResult 없으므로 예외 발생을 테스트하면 됨
        mvc.perform(
            post("/api/v1/user/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"test123\",\"email\":\"test@test.com\"}")
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @DisplayName("로그인")
    fun t2() {
        // 1. [준비] t1 데이터는 지워졌으므로, t2를 위해 다시 가입시켜야 합니다!
        // join() 결과를 바로 받아서 쓰면 DB 조회 에러 걱정이 없습니다.
        val user = userService.join("usernew", "1234", "test@test.com")

        // 2. [요청]
        val resultActions = mvc
            .perform(
                post("/api/v1/user/login")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "loginId": "usernew",
                                            "password": "1234"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        // 3. [검증]
        // user 변수에 이미 정보가 있으므로 findByLoginId를 또 할 필요가 없습니다.
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("login"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${user.loginId}님 환영합니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.user").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.user.id").value(user.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.user.loginId").value(user.loginId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.apiKey").value(user.apiKey))
    }

    @Test
    @DisplayName("로그아웃")
    fun t2_1() {
        // 1. [준비] 회원 생성
        userService.join("usernew", "1234", "test@test.com")

        // 2. [로그인] accessToken 쿠키 획득
        val loginResult = mvc
            .perform(
                post("/api/v1/user/login")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "loginId": "usernew",
                                            "password": "1234"
                                        }
                                        
                                        """.trimIndent()
                    )
            )

        // 로그인 결과에서 쿠키 추출
        val accessTokenCookie = checkNotNull(
            loginResult
                .andReturn()
                .response
                .getCookie("accessToken")
        )

        val logoutResult = mvc
            .perform(
                post("/api/v1/user/logout")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .cookie(accessTokenCookie)
            )
            .andDo(MockMvcResultHandlers.print())

        // 4. [검증]
        logoutResult
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("logout"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("로그아웃 되었습니다."))
            .andExpect(MockMvcResultMatchers.cookie().maxAge("accessToken", 0))
    }

    @Test
    @DisplayName("내 정보")
    fun t3() {
        // 1. [준비] BaseInitData의 user1 사용 (없으면 생성)
        val user = userService.findByLoginId("user1")
            ?: userService.join("user1", "1234", "user1@test.com")


        // 2. [준비] 로그인 API 호출하여 쿠키에 accessToken 획득
        val loginResult = mvc
            .perform(
                post("/api/v1/user/login")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "loginId": "user1",
                                            "password": "1234"
                                        }
                                        
                                        """.trimIndent()
                    )
            )

        // 로그인 응답의 쿠키에서 accessToken 추출
        val accessTokenCookie = checkNotNull(
            loginResult
                .andReturn()
                .response
                .getCookie("accessToken")
        )

        val resultActions = mvc
            .perform(
                get("/api/v1/user/me")
                    .cookie(accessTokenCookie) // 중요: 획득한 토큰 쿠키 전달
            )
            .andDo(MockMvcResultHandlers.print())

        // 4. [검증] 응답 검증
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("me"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(user.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.loginId").value("user1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(user.email))
    }

    @Test
    @DisplayName("이메일 수정")
    fun t4() {
        // 1. [준비] 회원 생성
        val user = userService.findByLoginId("user1")
            ?: userService.join("user1", "1234", "user1@test.com")

        val originalEmail = user.email

        // 2. [준비] 로그인 API 호출하여 쿠키에 accessToken 획득
        val loginResult = mvc
            .perform(
                post("/api/v1/user/login")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "loginId": "user1",
                                            "password": "1234"
                                        }
                                        
                                        """.trimIndent()
                    )
            )

        val accessTokenCookie = checkNotNull(
            loginResult
                .andReturn()
                .response
                .getCookie("accessToken")
        )

        val resultActions = mvc
            .perform(
                patch("/api/v1/user/me")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .cookie(accessTokenCookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "email": "updated@test.com"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        // 4. [검증] 응답 검증
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("updateProfile"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("회원정보가 수정되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.loginId").value("user1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value("updated@test.com"))

        // 5. [검증] DB에서 실제로 수정되었는지 확인
        val afterUser = checkNotNull(userService.findByLoginId("user1"))
        Assertions.assertNotEquals(originalEmail, afterUser.email, "이메일이 수정되어야 합니다")
        Assertions.assertEquals("updated@test.com", afterUser.email, "이메일이 올바르게 수정되어야 합니다")
    }

    @Test
    @DisplayName("비밀번호 변경")
    fun t5() {
        val loginId = "passwordChangeUser"
        val oldPassword = "1234"
        val newPassword = "newpassword123"

        // 1. [준비] 회원 생성
        userService.findByLoginId(loginId) ?: userService.join(loginId, oldPassword, "passwordChange@test.com")

        // 2. [준비] 로그인 API 호출하여 쿠키에 accessToken 획득
        val loginResult = mvc
            .perform(
                post("/api/v1/user/login")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "loginId": "$loginId",
                                            "password": "$oldPassword"
                                        }
                                        
                                        """.trimIndent()
                    )
            )

        val accessTokenCookie = checkNotNull(
            loginResult
                .andReturn()
                .response
                .getCookie("accessToken")
        )

        val resultActions = mvc
            .perform(
                patch("/api/v1/user/me/password")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .cookie(accessTokenCookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                        {
                                            "currentPassword": "$oldPassword",
                                            "newPassword": "$newPassword"
                                        }
                                        
                                        """.trimIndent()
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        // 4. [검증] 응답 검증
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("changePassword"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("비밀번호가 변경되었습니다."))

        // 5. [검증] 새 비밀번호로 로그인 가능한지 확인
        mvc.perform(
            post("/api/v1/user/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {
                                            "loginId": "$loginId",
                                            "password": "$newPassword"
                                        }
                                        
                                        """.trimIndent()
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
    }

    @Test
    @DisplayName("회원 탈퇴")
    fun t6() {
        // 회원 생성 및 로그인하여 accessToken 발급
        val user = userService.join("deleteUser", "1234", "delete@test.com")

        val loginResult = mvc.perform(
            post("/api/v1/user/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                            "loginId": "deleteUser",
                            "password": "1234"
                        }
                        
                        """.trimIndent()
                )
        )

        val accessTokenCookie = checkNotNull(
            loginResult
                .andReturn()
                .response
                .getCookie("accessToken")
        )
        // 회원 탈퇴
        val resultActions = mvc.perform(
            delete("/api/v1/user/me")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .cookie(accessTokenCookie)
        )
            .andDo(MockMvcResultHandlers.print())

        // 응답 및 쿠키 삭제 확인
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("deleteMe"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${user.loginId}님의 정보입니다."))
            .andExpect(MockMvcResultMatchers.cookie().value("accessToken", "")) // 토큰 쿠키 비워졌는지 확인

        // DB에서 실제로 삭제(또는 논리적 삭제)되었는지 확인
        Assertions.assertNull(userService.findByLoginId("deleteUser"), "회원이 삭제되어야 합니다.")
    }

    @Test
    @DisplayName("비밀번호 확인 - 성공")
    fun t7() {
        // 회원 생성 및 로그인
        userService.join("verifyUser", "1234", "verify@test.com")

        val loginResult = mvc.perform(
            post("/api/v1/user/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                            "loginId": "verifyUser",
                            "password": "1234"
                        }
                        
                        """.trimIndent()
                )
        )

        val accessTokenCookie = checkNotNull(
            loginResult
                .andReturn()
                .response
                .getCookie("accessToken")
        )
        // 올바른 비밀번호로 확인 요청 (POST /api/v1/user/me/verify-password)
        val resultActions = mvc.perform(
            post("/api/v1/user/me/verify-password")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .cookie(accessTokenCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                            "password": "1234"
                        }
                        
                        """.trimIndent()
                )
        )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1UserController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("verifyPassword"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("비밀번호가 확인되었습니다."))
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    fun t8() {
        // 회원 생성
        userService.findByLoginId("wrongPassUser")
            ?: userService.join("wrongPassUser", "1234", "wrongpass@test.com")


        // 틀린 비밀번호로 로그인 시도
        mvc.perform(
            post("/api/v1/user/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                            "loginId": "wrongPassUser",
                            "password": "wrongpassword"
                        }
                        
                        """.trimIndent()
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized()) // 400 대신 401을 기대하도록 수정
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("401-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("비밀번호가 일치하지 않습니다."))
    }

    @Test
    @DisplayName("인증되지 않은 사용자의 내 정보 조회 실패")
    fun t9() {
        // accessToken 쿠키 없이 /me 엔드포인트에 접근
        mvc.perform(
            get("/api/v1/user/me")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is4xxClientError())
    }
}
