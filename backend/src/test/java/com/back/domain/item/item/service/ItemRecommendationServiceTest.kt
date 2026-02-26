package com.back.domain.item.item.service

import com.back.domain.item.item.dto.ItemCycleRecommendResponse
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.google.genai.Client
import com.google.genai.Models
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.contains
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import tools.jackson.databind.ObjectMapper

@ExtendWith(MockitoExtension::class)
@DisplayName("ItemRecommendationService 테스트")
internal class ItemRecommendationServiceTest {

    // Nullable 타입(?)과 초기화(= null) 대신 lateinit var를 사용하여 불필요한 null 단언(!!) 제거
    @Mock
    private lateinit var genAiClient: Client

    @Mock
    private lateinit var genAiSystemConfig: GenerateContentConfig

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @Mock
    private lateinit var models: Models

    @Mock
    private lateinit var generateContentResponse: GenerateContentResponse

    @InjectMocks
    private lateinit var itemRecommendationService: ItemRecommendationService

    // Kotlin 네이밍 컨벤션에 맞게 카멜 케이스로 변경
    private val geminiModel = "gemini-2.5-flash-lite"

    @BeforeEach
    fun setUp() {
        // @Throws 어노테이션 제거 및 apply 스코프 함수를 사용하여 리플렉션 코드 간소화
        Client::class.java.getDeclaredField("models").apply {
            isAccessible = true
            set(genAiClient, models)
        }
    }

    // == 정상 응답 테스트 ==
    @Test
    @DisplayName("AI 추천 성공 - 일 단위 (칫솔)")
    fun getItemCycleRecommend_Success_Days() {
        //  이스케이프 문자(\) 대신 Kotlin의 Raw String(""")을 사용하여 JSON 가독성 향상
        val itemName = "칫솔"
        val aiResponse = """{"cycleValue": 90, "cycleUnit": "d"}"""
        val expectedResponse = ItemCycleRecommendResponse(90, "d")

        // 불필요한 제네릭 타입(<String?> 등) 제거, Kotlin의 타입 추론 활용
        given(generateContentResponse.text()).willReturn(aiResponse)
        given(
            models.generateContent(
                eq(geminiModel),
                anyString(),
                eq(genAiSystemConfig)
            )
        ).willReturn(generateContentResponse)

        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse::class.java))
            .willReturn(expectedResponse)

        val result = itemRecommendationService.getItemCycleRecommend(itemName)

        assertThat(result).isNotNull
        assertThat(result.cycleValue).isEqualTo(90)
        assertThat(result.cycleUnit).isEqualTo("d")

        verify(models, times(1)).generateContent(
            anyString(),
            anyString(),
            any()
        )
    }

    @Test
    @DisplayName("AI 추천 성공 - 개월 단위 (수세미)")
    fun getItemCycleRecommend_Success_Months() {
        val itemName = "수세미"
        val aiResponse = """{"cycleValue": 1, "cycleUnit": "m"}"""
        val expectedResponse = ItemCycleRecommendResponse(1, "m")

        given(generateContentResponse.text()).willReturn(aiResponse)
        given(
            models.generateContent(
                eq(geminiModel),
                anyString(),
                eq(genAiSystemConfig)
            )
        ).willReturn(generateContentResponse)

        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse::class.java))
            .willReturn(expectedResponse)

        val result = itemRecommendationService.getItemCycleRecommend(itemName)

        assertThat(result).isNotNull
        assertThat(result.cycleValue).isEqualTo(1)
        assertThat(result.cycleUnit).isEqualTo("m")
    }

    @Test
    @DisplayName("AI 추천 성공 - 년 단위 (매트리스)")
    fun getItemCycleRecommend_Success_Years() {
        val itemName = "매트리스"
        val aiResponse = """{"cycleValue": 10, "cycleUnit": "y"}"""
        val expectedResponse = ItemCycleRecommendResponse(10, "y")

        given(generateContentResponse.text()).willReturn(aiResponse)
        given(
            models.generateContent(
                eq(geminiModel),
                anyString(),
                eq(genAiSystemConfig)
            )
        ).willReturn(generateContentResponse)

        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse::class.java))
            .willReturn(expectedResponse)

        val result = itemRecommendationService.getItemCycleRecommend(itemName)

        assertThat(result).isNotNull
        assertThat(result.cycleValue).isEqualTo(10)
        assertThat(result.cycleUnit).isEqualTo("y")
    }

    @Test
    @DisplayName("AI 추천 성공 - JSON 앞뒤에 설명 텍스트 있는 경우")
    fun getItemCycleRecommend_Success_WithExtraText() {
        val itemName = "칫솔"
        val aiResponseWithExtra = """여기는 칫솔의 권장 교체 주기입니다. {"cycleValue": 90, "cycleUnit": "d"} 참고하세요."""
        val cleanedJson = """{"cycleValue": 90, "cycleUnit": "d"}"""
        val expectedResponse = ItemCycleRecommendResponse(90, "d")

        given(generateContentResponse.text()).willReturn(aiResponseWithExtra)
        given(
            models.generateContent(
                eq(geminiModel),
                anyString(),
                eq(genAiSystemConfig)
            )
        ).willReturn(generateContentResponse)

        given(objectMapper.readValue(cleanedJson, ItemCycleRecommendResponse::class.java))
            .willReturn(expectedResponse)

        val result = itemRecommendationService.getItemCycleRecommend(itemName)

        assertThat(result).isNotNull
        assertThat(result.cycleValue).isEqualTo(90)
        assertThat(result.cycleUnit).isEqualTo("d")
    }

    // == 예외 상황 테스트 ==
    @Test
    @DisplayName("AI 응답 실패 - 빈 응답")
    fun getItemCycleRecommend_Failure_EmptyResponse() {
        val itemName = "알 수 없는 물건"
        given(generateContentResponse.text()).willReturn("")
        given(
            models.generateContent(
                eq(geminiModel),
                anyString(),
                eq(genAiSystemConfig)
            )
        ).willReturn(generateContentResponse)

        // [변경사항] ThrowableAssert.ThrowingCallable 없이 Kotlin 후행 람다(trailing lambda) 적용
        assertThatThrownBy {
            itemRecommendationService.getItemCycleRecommend(itemName)
        }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.AI_NO_RESPONSE.message)
    }

    @Test
    @DisplayName("AI 응답 실패 - null 응답")
    fun getItemCycleRecommend_Failure_NullResponse() {
        val itemName = "알 수 없는 물건"
        given(generateContentResponse.text()).willReturn(null)
        given(
            models.generateContent(
                eq(geminiModel),
                anyString(),
                eq(genAiSystemConfig)
            )
        ).willReturn(generateContentResponse)

        assertThatThrownBy {
            itemRecommendationService.getItemCycleRecommend(itemName)
        }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.AI_NO_RESPONSE.message)
    }

    @Test
    @DisplayName("AI 응답 실패 - Not Found 응답")
    fun getItemCycleRecommend_Failure_NotFound() {
        val itemName = "이상한물건"
        val aiResponse = "Not Found"
        given(generateContentResponse.text()).willReturn(aiResponse)
        given(
            models.generateContent(
                eq(geminiModel),
                anyString(),
                eq(genAiSystemConfig)
            )
        ).willReturn(generateContentResponse)

        assertThatThrownBy {
            itemRecommendationService.getItemCycleRecommend(itemName)
        }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.AI_ITEM_NOT_FOUND.message)
    }

    @Test
    @DisplayName("AI 응답 실패 - 유효하지 않은 JSON 형식")
    fun getItemCycleRecommend_Failure_InvalidJson() {
        val itemName = "칫솔"
        val aiResponse = "이것은 JSON이 아닙니다"
        given(generateContentResponse.text()).willReturn(aiResponse)
        given(
            models.generateContent(
                eq(geminiModel),
                anyString(),
                eq(genAiSystemConfig)
            )
        ).willReturn(generateContentResponse)

        assertThatThrownBy {
            itemRecommendationService.getItemCycleRecommend(itemName)
        }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.AI_INVALID_JSON.message)
    }

    @Test
    @DisplayName("AI 응답 실패 - JSON 파싱 오류")
    fun getItemCycleRecommend_Failure_JsonParsingError() {
        val itemName = "칫솔"
        val aiResponse = """{"cycleValue": 90, "cycleUnit": "d"}"""

        given(generateContentResponse.text()).willReturn(aiResponse)
        given(
            models.generateContent(
                eq(geminiModel),
                anyString(),
                eq(genAiSystemConfig)
            )
        ).willReturn(generateContentResponse)

        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse::class.java))
            .willThrow(RuntimeException("JSON 파싱 실패"))

        assertThatThrownBy {
            itemRecommendationService.getItemCycleRecommend(itemName)
        }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.JSON_PARSING_ERROR.message)
    }

    @Test
    @DisplayName("AI 응답 실패 - 중괄호가 없는 응답")
    fun getItemCycleRecommend_Failure_NoBraces() {
        val itemName = "칫솔"
        val aiResponse = "cycleValue: 90, cycleUnit: d"
        given(generateContentResponse.text()).willReturn(aiResponse)
        given(
            models.generateContent(
                eq(geminiModel),
                anyString(),
                eq(genAiSystemConfig)
            )
        ).willReturn(generateContentResponse)

        assertThatThrownBy {
            itemRecommendationService.getItemCycleRecommend(itemName)
        }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.AI_INVALID_JSON.message)
    }

    @Test
    @DisplayName("AI 응답 실패 - 시작 중괄호만 있는 경우")
    fun getItemCycleRecommend_Failure_OnlyOpeningBrace() {
        val itemName = "칫솔"
        val aiResponse = "{cycleValue: 90"
        given(generateContentResponse.text()).willReturn(aiResponse)
        given(
            models.generateContent(
                eq(geminiModel),
                anyString(),
                eq(genAiSystemConfig)
            )
        ).willReturn(generateContentResponse)

        assertThatThrownBy {
            itemRecommendationService.getItemCycleRecommend(itemName)
        }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.AI_INVALID_JSON.message)
    }

    // == 다양한 소모품 테스트 ==
    @Test
    @DisplayName("다양한 소모품 추천 - 샤워타올")
    fun getItemCycleRecommend_Various_Towel() {
        val itemName = "샤워타올"
        val aiResponse = """{"cycleValue": 6, "cycleUnit": "m"}"""
        val expectedResponse = ItemCycleRecommendResponse(6, "m")

        given(generateContentResponse.text()).willReturn(aiResponse)
        given(
            models.generateContent(
                eq(geminiModel),
                anyString(),
                eq(genAiSystemConfig)
            )
        ).willReturn(generateContentResponse)

        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse::class.java))
            .willReturn(expectedResponse)

        val result = itemRecommendationService.getItemCycleRecommend(itemName)

        assertThat(result).isNotNull
        assertThat(result.cycleValue).isEqualTo(6)
        assertThat(result.cycleUnit).isEqualTo("m")
    }

    @Test
    @DisplayName("다양한 소모품 추천 - 베개")
    fun getItemCycleRecommend_Various_Pillow() {
        val itemName = "베개"
        val aiResponse = """{"cycleValue": 2, "cycleUnit": "y"}"""
        val expectedResponse = ItemCycleRecommendResponse(2, "y")

        given(generateContentResponse.text()).willReturn(aiResponse)
        given(
            models.generateContent(
                eq(geminiModel),
                anyString(),
                eq(genAiSystemConfig)
            )
        ).willReturn(generateContentResponse)

        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse::class.java))
            .willReturn(expectedResponse)

        val result = itemRecommendationService.getItemCycleRecommend(itemName)

        assertThat(result).isNotNull
        assertThat(result.cycleValue).isEqualTo(2)
        assertThat(result.cycleUnit).isEqualTo("y")
    }

    @Test
    @DisplayName("다양한 소모품 추천 - 마스크")
    fun getItemCycleRecommend_Various_Mask() {
        val itemName = "마스크"
        val aiResponse = """{"cycleValue": 1, "cycleUnit": "d"}"""
        val expectedResponse = ItemCycleRecommendResponse(1, "d")

        given(generateContentResponse.text()).willReturn(aiResponse)
        given(
            models.generateContent(
                eq(geminiModel),
                anyString(),
                eq(genAiSystemConfig)
            )
        ).willReturn(generateContentResponse)

        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse::class.java))
            .willReturn(expectedResponse)

        val result = itemRecommendationService.getItemCycleRecommend(itemName)

        assertThat(result).isNotNull
        assertThat(result.cycleValue).isEqualTo(1)
        assertThat(result.cycleUnit).isEqualTo("d")
    }

    // == 특수 케이스 테스트 ==
    @Test
    @DisplayName("프롬프트 생성 검증 - 아이템 이름이 프롬프트에 포함되는지 확인")
    fun getItemCycleRecommend_PromptContainsItemName() {
        val itemName = "칫솔"
        val aiResponse = """{"cycleValue": 90, "cycleUnit": "d"}"""
        val expectedResponse = ItemCycleRecommendResponse(90, "d")

        given(generateContentResponse.text()).willReturn(aiResponse)
        given(
            models.generateContent(
                eq(geminiModel),
                contains(itemName),
                eq(genAiSystemConfig)
            )
        ).willReturn(generateContentResponse)

        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse::class.java))
            .willReturn(expectedResponse)

        val result = itemRecommendationService.getItemCycleRecommend(itemName)

        assertThat(result).isNotNull
        verify(models).generateContent(
            eq(geminiModel),
            contains(itemName),
            eq(genAiSystemConfig)
        )
    }

    @Test
    @DisplayName("동일한 아이템으로 여러 번 요청 시 매번 AI 호출")
    fun getItemCycleRecommend_MultipleCallsForSameItem() {
        val itemName = "칫솔"
        val aiResponse = """{"cycleValue": 90, "cycleUnit": "d"}"""
        val expectedResponse = ItemCycleRecommendResponse(90, "d")

        given(generateContentResponse.text()).willReturn(aiResponse)
        given(
            models.generateContent(
                eq(geminiModel),
                anyString(),
                eq(genAiSystemConfig)
            )
        ).willReturn(generateContentResponse)

        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse::class.java))
            .willReturn(expectedResponse)

        itemRecommendationService.getItemCycleRecommend(itemName)
        itemRecommendationService.getItemCycleRecommend(itemName)
        itemRecommendationService.getItemCycleRecommend(itemName)

        verify(models, times(3)).generateContent(
            anyString(),
            anyString(),
            any()
        )
    }
}