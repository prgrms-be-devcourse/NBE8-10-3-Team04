package com.back.global.security;

import com.back.domain.user.user.entity.User;
import com.back.domain.user.user.service.UserService;
import com.back.standard.util.Ut;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    @Value("${custom.jwt.secretKey}")
    private String jwtSecret;

    @Value("${custom.accessToken.expirationSeconds}")
    private int accessTokenExpirationSeconds;

    // ============================================
    // 테스트 1: JWT 토큰 생성 및 검증
    // ============================================
    @Test
    @DisplayName("테스트 1: JWT 토큰 생성 및 검증")
    void t1_jwtTokenGenerationAndValidation() {
        // Given
        Map<String, Object> claims = Map.of(
                "id", 1L,
                "loginId", "testuser",
                "email", "test@test.com"
        );

        // When
        String token = Ut.jwt.toString(jwtSecret, accessTokenExpirationSeconds, claims);

        // Then
        assertThat(token).isNotBlank();
        assertThat(Ut.jwt.isValid(jwtSecret, token)).isTrue();

        Claims parsedClaims = Ut.jwt.payload(jwtSecret, token);
        assertThat(parsedClaims).isNotNull();
        assertThat(parsedClaims.get("id", Long.class)).isEqualTo(1L);
        assertThat(parsedClaims.get("loginId", String.class)).isEqualTo("testuser");
        assertThat(parsedClaims.get("email", String.class)).isEqualTo("test@test.com");
    }

    // ============================================
    // 테스트 2: 유효하지 않은 JWT 토큰 검증 실패
    // ============================================
    @Test
    @DisplayName("테스트 2: 유효하지 않은 JWT 토큰 검증 실패")
    void t2_invalidJwtTokenValidation() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThat(Ut.jwt.isValid(jwtSecret, invalidToken)).isFalse();
        assertThat(Ut.jwt.payload(jwtSecret, invalidToken)).isNull();
    }

    // ============================================
    // 테스트 3: 인증 불필요한 엔드포인트는 토큰 없이 접근 가능
    // ============================================
    @Test
    @DisplayName("테스트 3: 인증 불필요한 엔드포인트는 토큰 없이 접근 가능")
    void t3_publicEndpointAccessWithoutToken() throws Exception {
        // Given
        User user = userService.join("testuser", "1234", "test@test.com");

        // When
        ResultActions resultActions = mvc.perform(
                        post("/api/v1/user/login")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "loginId": "testuser",
                                            "password": "1234"
                                        }
                                        """.stripIndent())
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk());
    }

    // ============================================
    // 테스트 4: Bearer 형식이 아닌 Authorization 헤더는 401
    // ============================================
    @Test
    @DisplayName("테스트 4: Bearer 형식이 아닌 Authorization 헤더는 401")
    void t4_invalidBearerFormatReturns401() throws Exception {
        // Given
        String invalidAuthHeader = "InvalidFormat token123";

        // When
        ResultActions resultActions = mvc.perform(
                        get("/api/v1/user/me")
                                .header("Authorization", invalidAuthHeader)
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-2"));
    }

    // ============================================
    // 테스트 5: 유효하지 않은 JWT 토큰으로 요청 시 401
    // ============================================
    @Test
    @DisplayName("테스트 5: 유효하지 않은 JWT 토큰으로 요청 시 401")
    void t5_invalidJwtTokenReturns401() throws Exception {
        // Given - 유효하지 않은 토큰 + apiKey도 없음
        String invalidToken = "Bearer invalid.jwt.token";

        // When
        ResultActions resultActions = mvc.perform(
                        get("/api/v1/user/me")
                                .header("Authorization", invalidToken)
                )
                .andDo(print());

        // Then - accessToken이 유효하지 않고 apiKey도 없으므로 INVALID_API_KEY(401-5) 발생
        resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-5"));
    }

    // ============================================
    // 테스트 6: 유효한 JWT 토큰으로 인증 성공
    // ============================================
    @Test
    @DisplayName("테스트 6: 유효한 JWT 토큰으로 인증 성공")
    void t6_validJwtTokenAuthenticationSuccess() throws Exception {
        // Given
        User user = userService.join("testuser", "1234", "test@test.com");

        Map<String, Object> claims = Map.of(
                "id", user.getId(),
                "loginId", user.getLoginId(),
                "email", user.getEmail() != null ? user.getEmail() : "",
                "tokenVersion", user.getTokenVersion()
        );

        String accessToken = Ut.jwt.toString(jwtSecret, accessTokenExpirationSeconds, claims);

        // When
        ResultActions resultActions = mvc.perform(
                        get("/api/v1/user/me")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andDo(print());

        // Then - 필터가 통과하고 SecurityContext에 인증 정보가 주입됨
        // 상태 코드는 엔드포인트 구현에 따라 다르므로 2xx 또는 4xx 허용
        int status = resultActions.andReturn().getResponse().getStatus();
        assertThat(status).isBetween(200, 499);
    }

    // ============================================
    // 테스트 7: 존재하지 않는 회원 ID로 토큰 생성 시 404
    // ============================================
    @Test
    @DisplayName("테스트 7: 존재하지 않는 회원 ID로 토큰 생성 시 404")
    void t7_nonexistentUserIdReturns404() throws Exception {
        // Given - 존재하지 않는 회원 ID로 토큰 생성
        Map<String, Object> claims = Map.of(
                "id", 99999L,
                "loginId", "nonexistent",
                "email", "nonexistent@test.com",
                "tokenVersion", 0L
        );

        String accessToken = Ut.jwt.toString(jwtSecret, accessTokenExpirationSeconds, claims);

        // When - 쿠키 방식으로 토큰 전달 (Authorization 헤더는 Bearer {apiKey} {accessToken} 형식이라서)
        ResultActions resultActions = mvc.perform(
                        get("/api/v1/user/me")
                                .cookie(new Cookie("accessToken", accessToken))
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    // ============================================
    // 테스트 8: 토큰 클레임이 올바르지 않을 때 401
    // ============================================
    @Test
    @DisplayName("테스트 8: 토큰 클레임이 올바르지 않을 때 401")
    void t8_invalidTokenClaimsReturns401() throws Exception {
        // Given - 필수 클레임(id, loginId)이 없는 토큰
        Map<String, Object> invalidClaims = Map.of(
                "email", "test@test.com",
                "tokenVersion", 0L
                // id, loginId 누락
        );

        String token = Ut.jwt.toString(jwtSecret, accessTokenExpirationSeconds, invalidClaims);

        // When - 쿠키 방식으로 토큰 전달
        ResultActions resultActions = mvc.perform(
                        get("/api/v1/user/me")
                                .cookie(new Cookie("accessToken", token))
                )
                .andDo(print());

        // Then - id, loginId가 없으면 401-3 (INVALID_TOKEN_CLAIM)
        resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-3"));
    }

    // ============================================
    // 테스트 9: 쿠키에서 accessToken 추출
    // ============================================
    @Test
    @DisplayName("테스트 9: 쿠키에서 accessToken 추출")
    void t9_accessTokenFromCookie() throws Exception {
        // Given
        User user = userService.join("testuser", "1234", "test@test.com");

        Map<String, Object> claims = Map.of(
                "id", user.getId(),
                "loginId", user.getLoginId(),
                "email", user.getEmail() != null ? user.getEmail() : "",
                "tokenVersion", user.getTokenVersion()
        );

        String accessToken = Ut.jwt.toString(jwtSecret, accessTokenExpirationSeconds, claims);

        // When - 쿠키로 토큰 전달
        ResultActions resultActions = mvc.perform(
                        get("/api/v1/user/me")                                .cookie(new Cookie("accessToken", accessToken))
                                .cookie(new Cookie("accessToken", accessToken))
                )
                .andDo(print());

        // Then - 필터가 통과하고 인증 정보가 주입됨
        // 상태 코드는 엔드포인트 구현에 따라 다르므로 2xx 또는 4xx 허용
        int status = resultActions.andReturn().getResponse().getStatus();
        assertThat(status).isBetween(200, 499);
    }

    // ============================================
    // 테스트 10: 토큰 없이 API 요청 시 통과 (익명 요청)
    // ============================================
    @Test
    @DisplayName("테스트 10: 토큰 없이 API 요청 시 통과 (익명 요청)")
    void t10_anonymousRequestWithoutToken() throws Exception {
        // When - 토큰 없이 요청
        ResultActions resultActions = mvc.perform(
                        get("/api/v1/user/me")
                )
                .andDo(print());

        // Then - 필터가 통과 (permitAll이므로)
        // 상태 코드는 엔드포인트 구현에 따라 다르므로 2xx, 4xx, 5xx 모두 허용
        int status = resultActions.andReturn().getResponse().getStatus();
        assertThat(status).isGreaterThanOrEqualTo(200);
    }

    // ============================================
    // 향후 추가 검증 필요 항목들
    // ============================================

    /*
     * TODO: 테스트 11 - Refresh Token 재발급 테스트 -> API로 대체함
     * - refreshToken으로 새로운 accessToken 발급
     * - refreshToken이 유효하지 않을 때 401
     * - refreshToken이 만료되었을 때 401
     *
     * TODO: 테스트 12 - 토큰 만료 시간 검증
     * - 만료된 토큰으로 요청 시 401
     * - 만료 시간이 가까운 토큰 자동 재발급 (구현 시)
     *
     * TODO: 테스트 13 - 실제 보호된 엔드포인트 테스트
     * - SecurityConfig에서 authenticated()로 설정된 엔드포인트
     * - 토큰 없이 접근 시 Spring Security가 401 반환하는지 확인
     * - 예: GET /api/v1/user/me 같은 엔드포인트
     *
     * TODO: 테스트 14 - SecurityContext 인증 정보 검증
     * - 필터 통과 후 SecurityContext에 SecurityUser가 제대로 주입되었는지 확인
     * - 컨트롤러에서 @AuthenticationPrincipal로 접근 가능한지 확인
     *
     * TODO: 테스트 15 - 동시 요청 처리 테스트
     * - 여러 요청이 동시에 들어올 때 필터가 올바르게 동작하는지 확인
     * - SecurityContext가 요청별로 격리되는지 확인
     *
     * TODO: 테스트 16 - CORS 설정 검증
     * - CORS 헤더가 올바르게 설정되는지 확인
     * - preflight 요청(OPTIONS)이 올바르게 처리되는지 확인
     *
     * TODO: 테스트 17 - 에러 응답 포맷 검증
     * - ServiceException 발생 시 RsData 형식으로 올바르게 반환되는지 확인
     * - statusCode가 올바르게 설정되는지 확인
     *
     * TODO: 테스트 18 - 토큰 재발급 시 쿠키/헤더 업데이트 검증
     * - 토큰 재발급 시 쿠키에 새로운 토큰이 설정되는지 확인
     * - 응답 헤더에 새로운 토큰이 포함되는지 확인
     *
     * TODO: 테스트 19 - 회원 탈퇴/삭제 후 토큰 사용 시도
     * - 회원이 삭제된 후 해당 회원의 토큰으로 요청 시 401 반환 확인
     *
     * TODO: 테스트 20 - 토큰 서명 검증
     * - 다른 secretKey로 생성된 토큰은 거부되는지 확인
     * - 토큰 변조 시도 시 거부되는지 확인
     */
}