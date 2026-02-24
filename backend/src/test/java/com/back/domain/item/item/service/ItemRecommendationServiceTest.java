package com.back.domain.item.item.service;

import com.back.domain.item.item.dto.ItemCycleRecommendResponse;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemRecommendationService 테스트")
class ItemRecommendationServiceTest {

    @Mock
    private Client genAiClient;

    @Mock
    private GenerateContentConfig genAiSystemConfig;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ItemRecommendationService itemRecommendationService;

    @Mock
    private Models models;

    @Mock
    private GenerateContentResponse generateContentResponse;

    private final String GEMINI_MODEL = "gemini-2.5-flash-lite";

    @BeforeEach
    void setUp() throws Exception {
        // Client 객체 내부의 models 필드는 직접 주입이 어려우므로 Reflection을 사용하여 Mock 객체를 할당
        Field modelsField = Client.class.getDeclaredField("models");
        modelsField.setAccessible(true);
        modelsField.set(genAiClient, models);
    }

    // == 정상 응답 테스트 ==

    @Test
    @DisplayName("AI 추천 성공 - 일 단위 (칫솔)")
    void getItemCycleRecommend_Success_Days() throws Exception {
        // 테스트할 아이템 이름과 예상되는 AI JSON 응답 및 결과 객체 준비
        String itemName = "칫솔";
        String aiResponse = "{\"cycleValue\": 90, \"cycleUnit\": \"d\"}";
        ItemCycleRecommendResponse expectedResponse = new ItemCycleRecommendResponse(90, "d");

        // AI 모델이 텍스트 응답을 반환하도록 설정
        given(generateContentResponse.text()).willReturn(aiResponse);

        // Client의 models.generateContent 메서드가 호출되면 준비된 응답 객체를 반환하도록 설정
        given(models.generateContent(
                eq(GEMINI_MODEL),
                anyString(),
                eq(genAiSystemConfig)
        )).willReturn(generateContentResponse);

        // ObjectMapper가 JSON 문자열을 객체로 변환하는 동작 설정
        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse.class))
                .willReturn(expectedResponse);

        // 서비스 메서드 실행
        ItemCycleRecommendResponse result = itemRecommendationService.getItemCycleRecommend(itemName);

        // 결과가 null이 아니고, 주기 값과 단위가 예상값과 일치하는지 검증
        assertThat(result).isNotNull();
        assertThat(result.cycleValue()).isEqualTo(90);
        assertThat(result.cycleUnit()).isEqualTo("d");

        // AI 모델 호출이 1회 발생했는지 확인
        verify(models, times(1)).generateContent(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("AI 추천 성공 - 개월 단위 (수세미)")
    void getItemCycleRecommend_Success_Months() throws Exception {
        // 월 단위('m') 응답에 대한 테스트 데이터 설정
        String itemName = "수세미";
        String aiResponse = "{\"cycleValue\": 1, \"cycleUnit\": \"m\"}";
        ItemCycleRecommendResponse expectedResponse = new ItemCycleRecommendResponse(1, "m");

        // Mock 객체 동작 정의
        given(generateContentResponse.text()).willReturn(aiResponse);
        given(models.generateContent(
                eq(GEMINI_MODEL),
                anyString(),
                eq(genAiSystemConfig)
        )).willReturn(generateContentResponse);
        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse.class))
                .willReturn(expectedResponse);

        // 서비스 메서드 실행
        ItemCycleRecommendResponse result = itemRecommendationService.getItemCycleRecommend(itemName);

        // 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.cycleValue()).isEqualTo(1);
        assertThat(result.cycleUnit()).isEqualTo("m");
    }

    @Test
    @DisplayName("AI 추천 성공 - 년 단위 (매트리스)")
    void getItemCycleRecommend_Success_Years() throws Exception {
        // 년 단위('y') 응답에 대한 테스트 데이터 설정
        String itemName = "매트리스";
        String aiResponse = "{\"cycleValue\": 10, \"cycleUnit\": \"y\"}";
        ItemCycleRecommendResponse expectedResponse = new ItemCycleRecommendResponse(10, "y");

        // Mock 객체 동작 정의
        given(generateContentResponse.text()).willReturn(aiResponse);
        given(models.generateContent(
                eq(GEMINI_MODEL),
                anyString(),
                eq(genAiSystemConfig)
        )).willReturn(generateContentResponse);
        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse.class))
                .willReturn(expectedResponse);

        // 서비스 메서드 실행
        ItemCycleRecommendResponse result = itemRecommendationService.getItemCycleRecommend(itemName);

        // 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.cycleValue()).isEqualTo(10);
        assertThat(result.cycleUnit()).isEqualTo("y");
    }

    @Test
    @DisplayName("AI 추천 성공 - JSON 앞뒤에 설명 텍스트 있는 경우")
    void getItemCycleRecommend_Success_WithExtraText() throws Exception {
        // JSON 외에 불필요한 텍스트가 포함된 응답 시뮬레이션
        String itemName = "칫솔";
        String aiResponseWithExtra = "여기는 칫솔의 권장 교체 주기입니다. {\"cycleValue\": 90, \"cycleUnit\": \"d\"} 참고하세요.";
        String cleanedJson = "{\"cycleValue\": 90, \"cycleUnit\": \"d\"}";
        ItemCycleRecommendResponse expectedResponse = new ItemCycleRecommendResponse(90, "d");

        // AI가 설명이 포함된 텍스트를 반환하도록 설정
        given(generateContentResponse.text()).willReturn(aiResponseWithExtra);
        given(models.generateContent(
                eq(GEMINI_MODEL),
                anyString(),
                eq(genAiSystemConfig)
        )).willReturn(generateContentResponse);

        // 서비스 내부 로직이 JSON만 추출하여 매핑하므로, 순수 JSON 문자열에 대한 매핑 동작 정의
        given(objectMapper.readValue(cleanedJson, ItemCycleRecommendResponse.class))
                .willReturn(expectedResponse);

        // 서비스 메서드 실행
        ItemCycleRecommendResponse result = itemRecommendationService.getItemCycleRecommend(itemName);

        // 추출 및 파싱 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.cycleValue()).isEqualTo(90);
        assertThat(result.cycleUnit()).isEqualTo("d");
    }

    // == 예외 상황 테스트 ==

    @Test
    @DisplayName("AI 응답 실패 - 빈 응답")
    void getItemCycleRecommend_Failure_EmptyResponse() {
        // 빈 문자열이 반환되는 상황 설정
        String itemName = "알 수 없는 물건";
        given(generateContentResponse.text()).willReturn("");
        given(models.generateContent(
                eq(GEMINI_MODEL),
                anyString(),
                eq(genAiSystemConfig)
        )).willReturn(generateContentResponse);

        // 서비스 메서드 호출 시 AI_NO_RESPONSE 예외가 발생하는지 검증
        assertThatThrownBy(() -> itemRecommendationService.getItemCycleRecommend(itemName))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.AI_NO_RESPONSE.getMessage());
    }

    @Test
    @DisplayName("AI 응답 실패 - null 응답")
    void getItemCycleRecommend_Failure_NullResponse() {
        // null이 반환되는 상황 설정
        String itemName = "알 수 없는 물건";
        given(generateContentResponse.text()).willReturn(null);
        given(models.generateContent(
                eq(GEMINI_MODEL),
                anyString(),
                eq(genAiSystemConfig)
        )).willReturn(generateContentResponse);

        // 서비스 메서드 호출 시 AI_NO_RESPONSE 예외가 발생하는지 검증
        assertThatThrownBy(() -> itemRecommendationService.getItemCycleRecommend(itemName))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.AI_NO_RESPONSE.getMessage());
    }

    @Test
    @DisplayName("AI 응답 실패 - Not Found 응답")
    void getItemCycleRecommend_Failure_NotFound() {
        // "Not Found" 텍스트가 포함된 응답 상황 설정
        String itemName = "이상한물건";
        String aiResponse = "Not Found";
        given(generateContentResponse.text()).willReturn(aiResponse);
        given(models.generateContent(
                eq(GEMINI_MODEL),
                anyString(),
                eq(genAiSystemConfig)
        )).willReturn(generateContentResponse);

        // 서비스 메서드 호출 시 AI_ITEM_NOT_FOUND 예외가 발생하는지 검증
        assertThatThrownBy(() -> itemRecommendationService.getItemCycleRecommend(itemName))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.AI_ITEM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("AI 응답 실패 - 유효하지 않은 JSON 형식")
    void getItemCycleRecommend_Failure_InvalidJson() {
        // JSON 형식이 아닌 일반 텍스트 응답 설정
        String itemName = "칫솔";
        String aiResponse = "이것은 JSON이 아닙니다";
        given(generateContentResponse.text()).willReturn(aiResponse);
        given(models.generateContent(
                eq(GEMINI_MODEL),
                anyString(),
                eq(genAiSystemConfig)
        )).willReturn(generateContentResponse);

        // JSON 블록을 찾을 수 없으므로 AI_INVALID_JSON 예외 발생 검증
        assertThatThrownBy(() -> itemRecommendationService.getItemCycleRecommend(itemName))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.AI_INVALID_JSON.getMessage());
    }

    @Test
    @DisplayName("AI 응답 실패 - JSON 파싱 오류")
    void getItemCycleRecommend_Failure_JsonParsingError() throws Exception {
        // 정상적인 JSON 응답이지만 매핑 과정에서 오류가 발생하는 상황 시뮬레이션
        String itemName = "칫솔";
        String aiResponse = "{\"cycleValue\": 90, \"cycleUnit\": \"d\"}";

        given(generateContentResponse.text()).willReturn(aiResponse);
        given(models.generateContent(
                eq(GEMINI_MODEL),
                anyString(),
                eq(genAiSystemConfig)
        )).willReturn(generateContentResponse);

        // ObjectMapper가 변환 중 런타임 예외를 던지도록 설정 (BDDMockito.willThrow 사용)
        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse.class))
                .willThrow(new RuntimeException("JSON 파싱 실패"));

        // JSON_PARSING_ERROR 예외로 래핑되어 던져지는지 검증
        assertThatThrownBy(() -> itemRecommendationService.getItemCycleRecommend(itemName))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.JSON_PARSING_ERROR.getMessage());
    }

    @Test
    @DisplayName("AI 응답 실패 - 중괄호가 없는 응답")
    void getItemCycleRecommend_Failure_NoBraces() {
        // 중괄호가 누락된 잘못된 형식의 응답 설정
        String itemName = "칫솔";
        String aiResponse = "cycleValue: 90, cycleUnit: d";
        given(generateContentResponse.text()).willReturn(aiResponse);
        given(models.generateContent(
                eq(GEMINI_MODEL),
                anyString(),
                eq(genAiSystemConfig)
        )).willReturn(generateContentResponse);

        // 유효한 JSON 블록 추출 실패로 인한 AI_INVALID_JSON 예외 검증
        assertThatThrownBy(() -> itemRecommendationService.getItemCycleRecommend(itemName))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.AI_INVALID_JSON.getMessage());
    }

    @Test
    @DisplayName("AI 응답 실패 - 시작 중괄호만 있는 경우")
    void getItemCycleRecommend_Failure_OnlyOpeningBrace() {
        // 닫는 중괄호가 없는 불완전한 JSON 응답 설정
        String itemName = "칫솔";
        String aiResponse = "{cycleValue: 90";
        given(generateContentResponse.text()).willReturn(aiResponse);
        given(models.generateContent(
                eq(GEMINI_MODEL),
                anyString(),
                eq(genAiSystemConfig)
        )).willReturn(generateContentResponse);

        // 유효한 JSON 블록 추출 실패로 인한 AI_INVALID_JSON 예외 검증
        assertThatThrownBy(() -> itemRecommendationService.getItemCycleRecommend(itemName))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.AI_INVALID_JSON.getMessage());
    }

    // == 다양한 소모품 테스트 ==

    @Test
    @DisplayName("다양한 소모품 추천 - 샤워타올")
    void getItemCycleRecommend_Various_Towel() throws Exception {
        // 샤워타올에 대한 추천 테스트
        String itemName = "샤워타올";
        String aiResponse = "{\"cycleValue\": 6, \"cycleUnit\": \"m\"}";
        ItemCycleRecommendResponse expectedResponse = new ItemCycleRecommendResponse(6, "m");

        // Mock 동작 설정
        given(generateContentResponse.text()).willReturn(aiResponse);
        given(models.generateContent(
                eq(GEMINI_MODEL),
                anyString(),
                eq(genAiSystemConfig)
        )).willReturn(generateContentResponse);
        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse.class))
                .willReturn(expectedResponse);

        // 서비스 실행 및 검증
        ItemCycleRecommendResponse result = itemRecommendationService.getItemCycleRecommend(itemName);
        assertThat(result).isNotNull();
        assertThat(result.cycleValue()).isEqualTo(6);
        assertThat(result.cycleUnit()).isEqualTo("m");
    }

    @Test
    @DisplayName("다양한 소모품 추천 - 베개")
    void getItemCycleRecommend_Various_Pillow() throws Exception {
        // 베개에 대한 추천 테스트
        String itemName = "베개";
        String aiResponse = "{\"cycleValue\": 2, \"cycleUnit\": \"y\"}";
        ItemCycleRecommendResponse expectedResponse = new ItemCycleRecommendResponse(2, "y");

        // Mock 동작 설정
        given(generateContentResponse.text()).willReturn(aiResponse);
        given(models.generateContent(
                eq(GEMINI_MODEL),
                anyString(),
                eq(genAiSystemConfig)
        )).willReturn(generateContentResponse);
        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse.class))
                .willReturn(expectedResponse);

        // 서비스 실행 및 검증
        ItemCycleRecommendResponse result = itemRecommendationService.getItemCycleRecommend(itemName);
        assertThat(result).isNotNull();
        assertThat(result.cycleValue()).isEqualTo(2);
        assertThat(result.cycleUnit()).isEqualTo("y");
    }

    @Test
    @DisplayName("다양한 소모품 추천 - 마스크")
    void getItemCycleRecommend_Various_Mask() throws Exception {
        // 마스크에 대한 추천 테스트
        String itemName = "마스크";
        String aiResponse = "{\"cycleValue\": 1, \"cycleUnit\": \"d\"}";
        ItemCycleRecommendResponse expectedResponse = new ItemCycleRecommendResponse(1, "d");

        // Mock 동작 설정
        given(generateContentResponse.text()).willReturn(aiResponse);
        given(models.generateContent(
                eq(GEMINI_MODEL),
                anyString(),
                eq(genAiSystemConfig)
        )).willReturn(generateContentResponse);
        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse.class))
                .willReturn(expectedResponse);

        // 서비스 실행 및 검증
        ItemCycleRecommendResponse result = itemRecommendationService.getItemCycleRecommend(itemName);
        assertThat(result).isNotNull();
        assertThat(result.cycleValue()).isEqualTo(1);
        assertThat(result.cycleUnit()).isEqualTo("d");
    }

    // == 특수 케이스 테스트 ==

    @Test
    @DisplayName("프롬프트 생성 검증 - 아이템 이름이 프롬프트에 포함되는지 확인")
    void getItemCycleRecommend_PromptContainsItemName() throws Exception {
        // 요청한 아이템 이름이 실제로 AI 프롬프트에 포함되어 전달되는지 검증
        String itemName = "칫솔";
        String aiResponse = "{\"cycleValue\": 90, \"cycleUnit\": \"d\"}";
        ItemCycleRecommendResponse expectedResponse = new ItemCycleRecommendResponse(90, "d");

        given(generateContentResponse.text()).willReturn(aiResponse);
        // contains(itemName)을 사용하여 프롬프트 내용 검증
        given(models.generateContent(
                eq(GEMINI_MODEL),
                contains(itemName),
                eq(genAiSystemConfig)
        )).willReturn(generateContentResponse);
        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse.class))
                .willReturn(expectedResponse);

        ItemCycleRecommendResponse result = itemRecommendationService.getItemCycleRecommend(itemName);

        assertThat(result).isNotNull();
        verify(models).generateContent(eq(GEMINI_MODEL), contains(itemName), eq(genAiSystemConfig));
    }

    @Test
    @DisplayName("동일한 아이템으로 여러 번 요청 시 매번 AI 호출")
    void getItemCycleRecommend_MultipleCallsForSameItem() throws Exception {
        // 캐싱 로직이 없음을 확인하기 위해 동일 아이템으로 3번 호출 시도
        String itemName = "칫솔";
        String aiResponse = "{\"cycleValue\": 90, \"cycleUnit\": \"d\"}";
        ItemCycleRecommendResponse expectedResponse = new ItemCycleRecommendResponse(90, "d");

        given(generateContentResponse.text()).willReturn(aiResponse);
        given(models.generateContent(
                eq(GEMINI_MODEL),
                anyString(),
                eq(genAiSystemConfig)
        )).willReturn(generateContentResponse);
        given(objectMapper.readValue(aiResponse, ItemCycleRecommendResponse.class))
                .willReturn(expectedResponse);

        // 3회 호출
        itemRecommendationService.getItemCycleRecommend(itemName);
        itemRecommendationService.getItemCycleRecommend(itemName);
        itemRecommendationService.getItemCycleRecommend(itemName);

        // AI 모델도 3회 호출되었는지 검증
        verify(models, times(3)).generateContent(anyString(), anyString(), any());
    }
}