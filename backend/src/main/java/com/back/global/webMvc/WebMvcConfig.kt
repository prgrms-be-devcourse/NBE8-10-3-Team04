package com.back.global.webMvc

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        // 코틀린의 apply 스코프 함수를 사용하여 CorsRegistration 객체 설정을 그룹화
        registry.addMapping("/api/**").apply {

            // 프론트엔드 로컬 환경 및 EC2 배포 환경 CORS 허용
            allowedOrigins(
                "http://localhost:3000",
                "http://43.203.2.175:3000"
            )

            // 프론트엔드 Vercel 배포 시 동적 도메인 CORS 허용
            allowedOriginPatterns("https://*.vercel.app")

            // 명시적인 HTTP 메서드 허용
            allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")

            // 모든 헤더 허용 및 인증 정보(쿠키 등) 포함 허용
            allowedHeaders("*")
            allowCredentials(true)
        }
    }
}