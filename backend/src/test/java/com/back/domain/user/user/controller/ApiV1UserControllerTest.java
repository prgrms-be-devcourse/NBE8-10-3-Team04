package com.back.domain.user.user.controller;

import com.back.domain.user.user.entity.User;
import com.back.domain.user.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
public class ApiV1UserControllerTest {
    @Autowired
    private UserService userService;
    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("회원가입")
    void t1() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/user/signup")
                                .with(csrf()) //
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "loginId": "usernew",
                                            "password": "1234",
                                            "email": "test@test.com"
                                        }
                                        """.stripIndent()) //
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1UserController.class))
                .andExpect(handler().methodName("join"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").exists());

        resultActions
                .andExpect(jsonPath("$.data.loginId").value("usernew"));
    }

    @Test
    @DisplayName("회원가입: 유효성검증실패-아이디누락")
    void t1_2() throws Exception {
        // BindingResult 없으므로 예외 발생을 테스트하면 됨
        mvc.perform(post("/api/v1/user/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"test123\",\"email\":\"test@test.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("로그인")
    void t2() throws Exception {
        // 1. [준비] t1 데이터는 지워졌으므로, t2를 위해 다시 가입시켜야 합니다!
        // join() 결과를 바로 받아서 쓰면 DB 조회 에러 걱정이 없습니다.
        User user = userService.join("usernew", "1234", "test@test.com");

        // 2. [요청]
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/user/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "loginId": "usernew",
                                            "password": "1234"
                                        }
                                        """.stripIndent())
                )
                .andDo(print());

        // 3. [검증]
        // user 변수에 이미 정보가 있으므로 findByLoginId를 또 할 필요가 없습니다.

        resultActions
                .andExpect(handler().handlerType(ApiV1UserController.class))
                .andExpect(handler().methodName("login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%s님 환영합니다.".formatted(user.getLoginId())))
                .andExpect(jsonPath("$.data.user").exists())
                .andExpect(jsonPath("$.data.user.id").value(user.getId()))
                .andExpect(jsonPath("$.data.user.loginId").value(user.getLoginId()))
                .andExpect(jsonPath("$.data.apiKey").value(user.getApiKey()));
    }

    @Test
    @DisplayName("로그아웃")
    void t2_1() throws Exception {
        // 1. [준비] 회원 생성
        User user = userService.join("usernew", "1234", "test@test.com");

        // 2. [로그인] accessToken 쿠키 획득
        ResultActions loginResult = mvc
                .perform(
                        post("/api/v1/user/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "loginId": "usernew",
                                            "password": "1234"
                                        }
                                        """.stripIndent())
                );

        // 로그인 결과에서 쿠키 추출
        jakarta.servlet.http.Cookie accessTokenCookie = loginResult
                .andReturn()
                .getResponse()
                .getCookie("accessToken");

        // 3. [로그아웃] 쿠키를 가지고 요청
        assert accessTokenCookie != null;

        ResultActions logoutResult = mvc
                .perform(
                        post("/api/v1/user/logout")
                                .with(csrf())
                                .cookie(accessTokenCookie)
                )
                .andDo(print());

        // 4. [검증]
        logoutResult
                .andExpect(handler().handlerType(ApiV1UserController.class))
                .andExpect(handler().methodName("logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("로그아웃 되었습니다."))
                .andExpect(cookie().maxAge("accessToken", 0));
    }

    @Test
    @DisplayName("내 정보")
    void t3() throws Exception {
        // 1. [준비] BaseInitData의 user1 사용 (없으면 생성)
        User user = userService.findByLoginId("user1");
        if (user == null) {
            user = userService.join("user1", "1234", "user1@test.com");
        }

        // 2. [준비] 로그인 API 호출하여 쿠키에 accessToken 획득
        ResultActions loginResult = mvc
                .perform(
                        post("/api/v1/user/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "loginId": "user1",
                                            "password": "1234"
                                        }
                                        """.stripIndent())
                );

        // 로그인 응답의 쿠키에서 accessToken 추출
        jakarta.servlet.http.Cookie accessTokenCookie = loginResult
                .andReturn()
                .getResponse()
                .getCookie("accessToken");

        // 3. [요청] 내 정보 조회 (쿠키에 있는 토큰 사용)
        assert accessTokenCookie != null;

        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/user/me")
                                .cookie(accessTokenCookie) // 중요: 획득한 토큰 쿠키 전달
                )
                .andDo(print());

        // 4. [검증] 응답 검증
        resultActions
                .andExpect(handler().handlerType(ApiV1UserController.class))
                .andExpect(handler().methodName("me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.loginId").value("user1"))
                .andExpect(jsonPath("$.data.email").value(user.getEmail()));
    }

    @Test
    @DisplayName("이메일 수정")
    void t4() throws Exception {
        // 1. [준비] 회원 생성
        User user = userService.findByLoginId("user1");
        if (user == null) {
            user = userService.join("user1", "1234", "user1@test.com");
        }
        String originalEmail = user.getEmail();

        // 2. [준비] 로그인 API 호출하여 쿠키에 accessToken 획득
        ResultActions loginResult = mvc
                .perform(
                        post("/api/v1/user/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "loginId": "user1",
                                            "password": "1234"
                                        }
                                        """.stripIndent())
                );

        jakarta.servlet.http.Cookie accessTokenCookie = loginResult
                .andReturn()
                .getResponse()
                .getCookie("accessToken");

        // 3. [요청] 이메일 수정 (PATCH /api/v1/user/me)
        assert accessTokenCookie != null;

        ResultActions resultActions = mvc
                .perform(
                        patch("/api/v1/user/me")
                                .with(csrf())
                                .cookie(accessTokenCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "email": "updated@test.com"
                                        }
                                        """.stripIndent())
                )
                .andDo(print());

        // 4. [검증] 응답 검증
        resultActions
                .andExpect(handler().handlerType(ApiV1UserController.class))
                .andExpect(handler().methodName("updateProfile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-2"))
                .andExpect(jsonPath("$.msg").value("회원정보가 수정되었습니다."))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.loginId").value("user1"))
                .andExpect(jsonPath("$.data.email").value("updated@test.com"));

        // 5. [검증] DB에서 실제로 수정되었는지 확인
        User afterUser = java.util.Objects.requireNonNull(userService.findByLoginId("user1"));
        assertNotEquals(originalEmail, afterUser.getEmail(), "이메일이 수정되어야 합니다");
        assertEquals("updated@test.com", afterUser.getEmail(), "이메일이 올바르게 수정되어야 합니다");
    }

    @Test
    @DisplayName("비밀번호 변경")
    void t5() throws Exception {
        // 1. [준비] 회원 생성
        User user = userService.findByLoginId("user1");
        if (user == null) {
            user = userService.join("user1", "1234", "user1@test.com");
        }

        // 2. [준비] 로그인 API 호출하여 쿠키에 accessToken 획득
        ResultActions loginResult = mvc
                .perform(
                        post("/api/v1/user/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "loginId": "user2",
                                            "password": "1234"
                                        }
                                        """.stripIndent())
                );

        jakarta.servlet.http.Cookie accessTokenCookie = loginResult
                .andReturn()
                .getResponse()
                .getCookie("accessToken");

        // 3. [요청] 비밀번호 변경 (PATCH /api/v1/user/me/password)
        assert accessTokenCookie != null;

        ResultActions resultActions = mvc
                .perform(
                        patch("/api/v1/user/me/password")
                                .with(csrf())
                                .cookie(accessTokenCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "currentPassword": "1234",
                                            "newPassword": "newpassword123"
                                        }
                                        """.stripIndent())
                )
                .andDo(print());

        // 4. [검증] 응답 검증
        resultActions
                .andExpect(handler().handlerType(ApiV1UserController.class))
                .andExpect(handler().methodName("changePassword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-2"))
                .andExpect(jsonPath("$.msg").value("비밀번호가 변경되었습니다."));

        // 5. [검증] 새 비밀번호로 로그인 가능한지 확인
        mvc.perform(
                        post("/api/v1/user/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "loginId": "user2",
                                            "password": "newpassword123"
                                        }
                                        """.stripIndent())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));
    }

    @Test
    @DisplayName("회원 탈퇴")
    void t6() throws Exception {
        // 회원 생성 및 로그인하여 accessToken 발급
        User user = userService.join("deleteUser", "1234", "delete@test.com");

        ResultActions loginResult = mvc.perform(post("/api/v1/user/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "loginId": "deleteUser",
                            "password": "1234"
                        }
                        """.stripIndent()));

        jakarta.servlet.http.Cookie accessTokenCookie = loginResult
                .andReturn()
                .getResponse()
                .getCookie("accessToken");
        assert accessTokenCookie != null;

        // 회원 탈퇴
        ResultActions resultActions = mvc.perform(delete("/api/v1/user/me")
                        .with(csrf())
                        .cookie(accessTokenCookie))
                .andDo(print());

        // 응답 및 쿠키 삭제 확인
        resultActions
                .andExpect(handler().handlerType(ApiV1UserController.class))
                .andExpect(handler().methodName("deleteMe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%s님의 정보입니다.".formatted(user.getLoginId())))
                .andExpect(cookie().value("accessToken", "")); // 토큰 쿠키 비워졌는지 확인

        // DB에서 실제로 삭제(또는 논리적 삭제)되었는지 확인
        assertNull(userService.findByLoginId("deleteUser"), "회원이 삭제되어야 합니다.");
    }

    @Test
    @DisplayName("비밀번호 확인 - 성공")
    void t7() throws Exception {
        // 회원 생성 및 로그인
        User user = userService.join("verifyUser", "1234", "verify@test.com");

        ResultActions loginResult = mvc.perform(post("/api/v1/user/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "loginId": "verifyUser",
                            "password": "1234"
                        }
                        """.stripIndent()));

        jakarta.servlet.http.Cookie accessTokenCookie = loginResult
                .andReturn()
                .getResponse()
                .getCookie("accessToken");
        assert accessTokenCookie != null;

        // 올바른 비밀번호로 확인 요청 (POST /api/v1/user/me/verify-password)
        ResultActions resultActions = mvc.perform(post("/api/v1/user/me/verify-password")
                        .with(csrf())
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "password": "1234"
                        }
                        """.stripIndent()))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1UserController.class))
                .andExpect(handler().methodName("verifyPassword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("비밀번호가 확인되었습니다."));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void t8() throws Exception {
        // 회원 생성
        userService.join("wrongPassUser", "1234", "wrongpass@test.com");

        // 틀린 비밀번호로 로그인 시도
        mvc.perform(post("/api/v1/user/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "loginId": "wrongPassUser",
                            "password": "wrongpassword"
                        }
                        """.stripIndent()))
                .andDo(print())
                .andExpect(status().isUnauthorized()) // 400 대신 401을 기대하도록 수정
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("비밀번호가 일치하지 않습니다."));
    }

    @Test
    @DisplayName("인증되지 않은 사용자의 내 정보 조회 실패")
    void t9() throws Exception {
        // accessToken 쿠키 없이 /me 엔드포인트에 접근
        mvc.perform(get("/api/v1/user/me")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }
}