/*package com.back.global.security;

import com.back.domain.user.user.entity.User;
import com.back.domain.user.user.repository.UserRepository;
import com.back.domain.user.user.service.UserService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import com.back.standard.util.Ut;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;


@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;
    private final Rq rq;
    private final UserService userService;

    @Value("${custom.jwt.secretKey}")
    private String jwtSecret;

    @Override
    @NullMarked
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            work(request, response, filterChain);
        } catch (ServiceException e) {
            RsData<Void> rsData = e.getRsData();
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(rsData.getStatusCode());
            response.getWriter().write(Ut.json.toString(rsData));
        }
    }

    private void work(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        // 1) API 요청이 아닌 경우 패스
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2) 인증/인가가 필요없는 API 요청 패스
        if (List.of(
                "/api/v1/user/login",
                "/api/v1/user/signup",
                "/api/v1/user/refresh"
        ).contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3) apiKey, accessToken 추출
        String apiKey;
        String accessToken;
        String headerAuthorization = rq.getHeader("Authorization", "");

        // 3-1) Authorization 헤더일 때
        if (!headerAuthorization.isBlank()) {
            if (!headerAuthorization.startsWith("Bearer ")) {
                throw new ServiceException("401-2", "Authorization 헤더가 Bearer 형식이 아닙니다.");
            }
            String[] headerAuthorizationBits = headerAuthorization.split(" ", 3);

            apiKey = headerAuthorizationBits[1];
            accessToken = headerAuthorizationBits.length == 3 ? headerAuthorizationBits[2] : "";
        } else {
            // 3-2) 쿠키일 때
            apiKey = rq.getCookieValue("apiKey", "");
            accessToken = rq.getCookieValue("accessToken", "");
        }

        // apikey, accessToken이 모두 없으면 통과 (익명 요청)
        boolean isApiKeyExists = !apiKey.isBlank();
        boolean isAccessTokenExists = !accessToken.isBlank();
        if (!isApiKeyExists && !isAccessTokenExists) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4) accessToken, apiKey 둘 중 하나라도 있다면 인증/인가 수행
        User user = null;

        // 4-1) accessToken 우선 검증 및 파싱
        boolean isAccessTokenValid = false;
        if (isAccessTokenExists) {
            Claims claims = Ut.jwt.payload(jwtSecret, accessToken);
            if (claims != null) {
                // 5) 토큰에서 회원 ID 추출
                Long id = claims.get("id", Long.class);
                String loginId = claims.get("loginId", String.class);
                Long tokenVersion = claims.get("tokenVersion", Long.class);

                // 클레임 유효성 검사
                if (id == null || loginId == null) {
                    throw new ServiceException("401-1", "토큰 클레임이 올바르지 않습니다.");
                }

                // DB에서 실제 회원 조회
                user = userRepository.findById(id)
                        .orElseThrow(() -> new ServiceException("401-1", "존재하지 않는 회원입니다."));

                // tokenVersion 검증 - DB의 버전과 토큰의 버전이 다르면 토큰 무효화
                if (tokenVersion == null || !tokenVersion.equals(user.getTokenVersion())) {
                    throw new ServiceException("401-4", "토큰이 만료되었습니다. 다시 로그인해주세요.");
                }

                isAccessTokenValid = true;
            }
        }

        // 4-2) accessToken이 없으면 apiKey 탐색
        if (user == null) {
            user = userService.findByApiKey(apiKey)
                    .orElseThrow(() -> new ServiceException("401-3", "API 키가 유효하지 않습니다."));
        }

        // accessToken이 만료되었거나 유효하지 않다면 apiKey를 통해서 재발급
        if (isAccessTokenExists && !isAccessTokenValid) {
            String userAccessToken = userService.genAccessToken(user);

            rq.setCookie("accessToken", userAccessToken);
            rq.setHeader("Authorization", userAccessToken);
        }

        // 7) accessToken이 만료되었는지 확인 (선택적 - 만료 시간 체크)
        // 현재는 토큰이 유효하면 통과, 만료되면 위에서 이미 null 반환됨

        // 8) accessToken이 유효하지만 만료 시간이 가까우면 새로 발급 (선택적)
        // 필요시 토큰 만료 시간을 체크하여 재발급할 수 있음
        // 현재는 토큰이 유효하면 그대로 사용

        // 9) SecurityContext에 인증 정보 주입
        UserDetails securityUser = new SecurityUser(
                user.getId(),
                user.getLoginId(),
                "",
                user.getEmail() != null ? user.getEmail() : "",
                List.of() // 권한 없으면 빈 리스트
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(securityUser, securityUser.getPassword(),
                        securityUser.getAuthorities())
        );

        // 10) 다음 필터로 넘김
        filterChain.doFilter(request, response);
    }
}*/