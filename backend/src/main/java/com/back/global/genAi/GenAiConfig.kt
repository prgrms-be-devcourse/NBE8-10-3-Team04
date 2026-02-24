package com.back.global.genAi

import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Part
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GenAiConfig(
    // Java에서는 private 필드에 @Value를 달아 주입했지만, Kotlin에서는 생성자 주입을 사용하는 것을 권장
    @Value("\${google.gemini.api-key}")
    private val geminiApiKey: String
) {

    @Bean
    // Spring의 빈 생성 메서드는 Null을 반환하지 않으므로 Client? 대신 확실한 Client 타입을 사용
    // 또한, 함수 내용이 단일 객체 반환뿐이므로 중괄호({})와 return을 생략하고 '='로 간결하게 표현
    fun genAiClient(): Client = Client.builder()
        .apiKey(geminiApiKey)
        .build()

    @Bean
    fun genAiSystemConfig(): GenerateContentConfig {
        // Java에서는 여러 줄의 문자열을 +로 연결하고 내부에 이스케이프(\")를 써야 했지만,
        // Kotlin의 Raw String을 사용하면 이스케이프 문자 없이 여러 줄을 직관적으로 작성 가능
        val systemPrompt = """
            너는 살림 전문가야. 사용자가 소모품 이름을 말하면 권장 교체 주기를 알려줘야 해.
            응답은 반드시 다른 설명 없이 다음 JSON 형식으로만 보내줘: {"cycleValue": 자연수, "cycleUnit": "d(일)/m(개월)/y(년) 중 하나"}
            일반적인 소모품이 아니라면 Not Found로 응답해줘.
        """.trimIndent()

        return GenerateContentConfig.builder()
            .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
            .temperature(0.2f) // 창의성을 낮추어 JSON 형식을 더 잘 지키게 설정
            .build()
    }
}