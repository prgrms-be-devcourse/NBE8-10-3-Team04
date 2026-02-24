package com.back.global.springDoc

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SpringDocConfig {

    @Bean
    fun openAPI(): OpenAPI {
        // API 문서 정보 설정
        val info = Info()
            .title("API 서버")
            .version("beta")
            .description("API 서버 문서입니다.")

        // Security 스키마 설정 (JWT Bearer Auth)
        val securityScheme = SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .`in`(SecurityScheme.In.HEADER)
            .name("Authorization")

        // Security 요구사항 설정
        val securityRequirement = SecurityRequirement().addList("bearerAuth")

        // OpenAPI 객체 조립 및 반환
        return OpenAPI()
            .components(Components().addSecuritySchemes("bearerAuth", securityScheme))
            .security(listOf(securityRequirement))
            .info(info)
    }
}