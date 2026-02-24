package com.back.global.app

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class AppConfig(
    environment: Environment // 기존 Java의 @Autowired 세터 주입 대신 생성자 주입을 사용하여 의존성을 명확히 함
) {
    init {
        // 별도의 세터 메서드 없이 초기화 블록을 사용하여 companion object의 정적 변수에 할당
        Companion.environment = environment
    }

    // 메서드 중괄호와 return을 생략하고 단일 표현식 함수로 간결하게 작성
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    companion object {
        // Java의 private static 변수를 companion object 내부로 이동하고, Null 처리를 피하기 위해 lateinit var 적용
        private lateinit var environment: Environment

        // 정적(static) getter 메서드들을 Kotlin의 읽기 전용 프로퍼티(val)와 custom getter(get()) 문법으로 변환
        // environment.matchesProfiles() 대신 Spring 환경 배열인 activeProfiles.contains()를 사용하여 확인하도록 변경
        val isDev: Boolean
            get() = environment.activeProfiles.contains("dev")

        val isTest: Boolean
            get() = environment.activeProfiles.contains("test")

        val isProd: Boolean
            get() = environment.activeProfiles.contains("prod")

        // Java 코드와의 상호 운용성을 위해 @JvmStatic 어노테이션 추가
        @JvmStatic
        val isNotProd: Boolean
            get() = !isProd
    }
}