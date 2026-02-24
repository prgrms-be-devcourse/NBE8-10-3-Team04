package com.back.global.s3

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

/**
 * 1. 필드 주입에서 생성자 주입으로 변경되
 * - Java에서는 필드에 바로 @Value를 붙여 의존성을 주입받았지만, Kotlin의 주 생성자를 활용
 * 2. 불변성 보장:
 * - 모든 속성을 'val'로 선언하여 주입된 설정값이 애플리케이션 실행 중 변경되지 않도록 보장
 * 3. $ 기호 이스케이프(\$):
 * - Kotlin에서는 문자열 템플릿에 $ 기호를 사용하므로, Spring의 SpEL 형식인 ${...}을 사용하기 위해 \$ 기호로 이스케이프 처리
 */
@Configuration
class S3Config(
    @Value("\${spring.cloud.aws.credentials.access-key}")
    private val accessKey: String,

    @Value("\${spring.cloud.aws.credentials.secret-key}")
    private val secretKey: String,

    @Value("\${spring.cloud.aws.region.static}")
    private val region: String
) {
    /**
     * 단일 표현식 함수:
     * - 중괄호 '{}'와 'return' 키워드를 생략하고 '='를 사용
     * - 반환 타입은 명시하거나 추론할 수 있게 작성
     */
    @Bean
    fun s3Client(): S3Client = S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            )
        )
        .build()
}