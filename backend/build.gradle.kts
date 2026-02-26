plugins {
    java
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("jacoco") // 코드 커버리지 측정
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
}

group = "com"
version = "0.0.1-SNAPSHOT"
description = "backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// JaCoCo 도구 버전 명시
jacoco {
    toolVersion = "0.8.12"
}

tasks.test {
    useJUnitPlatform()
    ignoreFailures = true // 테스트가 실패해도 빌드를 계속 진행 (리포트 생성 가능)
    finalizedBy(tasks.jacocoTestReport) // 테스트 task가 끝나면 jacocoTestReport 실행
}

// 리포트 설정 및 불필요한 파일 제외
tasks.jacocoTestReport {
    dependsOn(tasks.test) // 리포트 생성 전 테스트가 먼저 실행되어야 함

    reports {
        xml.required.set(true)  // CI/CD 툴(SonarQube 등)에서 분석할 때 필요
        html.required.set(true) // 사람이 브라우저로 볼 때 필요
    }

    // 커버리지 측정에서 제외할 클래스들 설정 (Lombok, DTO, 설정파일 등)
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/dto/**",           // DTO는 로직이 없으므로 제외
                    "**/entity/**",        // QClass 등 제외 (필요시)
                    "**/config/**",        // 설정 파일 제외
                    "**/global/**",        // 전역 설정/예외 등 제외 (선택 사항)
                    "**/*Application*",    // 메인 애플리케이션 클래스 제외
                    "**/Q*",               // QueryDSL Q-Type 제외
                )
            }
        })
    )
}

// 커버리지 커트라인 설정: 기준 미달 시 빌드 실패
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            element = "CLASS"
            // 브랜치 커버리지 0% 이상 (최소한의 설정 예시)
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.00".toBigDecimal()
            }
        }
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

val mockitoAgent by configurations.creating {
    isTransitive = false
}

repositories {
    mavenCentral()
}

dependencies {
    // Web (REST API)
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Security (인증/인가)
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Validation (@Valid)
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // JWT (JJWT)
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Swagger (springdoc-openapi)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Monitoring (Prometheus + Grafana)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Devtools
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // DB Drivers (둘 중 필요한 것만 켜도 됨)
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.springframework.boot:spring-boot-h2console") // Spring Boot 4.0 부터 필요한 라이브러리
    runtimeOnly("com.mysql:mysql-connector-j")

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

    // Mail
    implementation("org.springframework.boot:spring-boot-starter-mail") // 메일서버와 연결해서 메일을 발송하는데 필요한 라이브러리

    // AWS 공식 Java SDK
    implementation("software.amazon.awssdk:s3:2.21.1")

    // Gemini
    implementation("com.google.genai:google-genai:1.32.0")

    implementation("org.apache.commons:commons-lang3:3.18.0")

    //kotlin
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    mockitoAgent("org.mockito:mockito-core:5.20.0")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-javaagent:${mockitoAgent.singleFile.absolutePath}")
}
