package com.back.global.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(
    private val customAuthenticationFilter: CustomAuthenticationFilter
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests{auth ->
                auth // 허용할 요청 설정
                    .requestMatchers("/favicon.ico").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    // 회원가입과 로그인은 누구나 들어갈 수 있게 허용
                    .requestMatchers("/api/v1/user/signup", "/api/v1/user/login").permitAll()
                    // 데이터 요청 가능여부 통과
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // 기타 api 요청은 인가 필요 -> 로그인하지 않으면 제한
                    .requestMatchers("/api/*/**").authenticated()
                    // 나머지 요청은 허용
                    .anyRequest().permitAll()
            }

            .headers { headers ->
                headers.frameOptions{ frame ->
                    frame.sameOrigin()
                }
            }
            .csrf { csrf -> csrf.disable() }
            .addFilterBefore(
                customAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }
}
