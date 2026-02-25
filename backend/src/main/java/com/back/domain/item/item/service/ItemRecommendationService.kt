package com.back.domain.item.item.service

import com.back.domain.item.item.dto.ItemCycleRecommendResponse
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * AI를 활용한 아이템 추천 서비스
 * - 아이템 교체 주기 추천
 */
@Service
class ItemRecommendationService(
    private val genAiClient: Client,
    private val genAiSystemConfig: GenerateContentConfig,
    private val objectMapper: ObjectMapper
) {
    // @Slf4j를 대체하는 코틀린의 로거를 사용
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val AI_TIMEOUT_SECONDS = 10L
        private const val GEMINI_MODEL = "gemini-2.5-flash-lite"
    }

    /**
     * AI를 통해 아이템의 권장 교체 주기를 추천받습니다.
     */
    fun getItemCycleRecommend(itemName: String): ItemCycleRecommendResponse {
        return try {
            CompletableFuture.supplyAsync {
                genAiClient.models.generateContent(
                    GEMINI_MODEL,
                    buildPrompt(itemName),
                    genAiSystemConfig
                )
            }
                .orTimeout(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .thenApply { response -> parseJson(response.text()) }
                .join()
        } catch (e: CompletionException) {
            handleCompletionException(e)
        }
    }

    /**
     * AI 요청을 위한 프롬프트 생성
     */
    private fun buildPrompt(itemName: String): String {
        return "${itemName}의 권장 교체 주기를 알려줘."
    }

    /**
     * CompletionException 처리
     */
    private fun handleCompletionException(e: CompletionException): Nothing {
        val cause = e.cause

        when (cause) {
            is ServiceException -> throw cause
            is TimeoutException -> {
                log.error("AI 응답 타임아웃: {}초 초과", AI_TIMEOUT_SECONDS)
                throw ServiceException(ErrorCode.AI_TIMEOUT)
            }
            else -> {
                log.error("AI 처리 중 예상치 못한 오류 발생", cause)
                throw ServiceException(ErrorCode.AI_ERROR)
            }
        }
    }

    /**
     * AI 응답 JSON 파싱
     */
    private fun parseJson(rawText: String?): ItemCycleRecommendResponse {
        if (rawText.isNullOrBlank()) {
            log.error("AI로부터 빈 응답을 받음")
            throw ServiceException(ErrorCode.AI_NO_RESPONSE)
        }

        if (rawText.contains("Not Found")) {
            log.warn("AI가 해당 아이템의 권장 주기를 찾을 수 없음: {}", rawText)
            throw ServiceException(ErrorCode.AI_ITEM_NOT_FOUND)
        }

        val cleanedJson = extractJsonBlock(rawText)

        return try {
            objectMapper.readValue(cleanedJson, ItemCycleRecommendResponse::class.java)
        } catch (e: Exception) {
            log.error("AI 응답 JSON 파싱 실패. 원본 텍스트: {}", rawText, e)
            throw ServiceException(ErrorCode.JSON_PARSING_ERROR)
        }
    }

    /**
     * 원본 텍스트에서 JSON 블록만 추출
     */
    private fun extractJsonBlock(rawText: String): String {
        val start = rawText.indexOf("{")
        val end = rawText.lastIndexOf("}")

        if (start == -1 || end == -1 || start >= end) {
            log.error("AI 응답에서 유효한 JSON 블록을 찾을 수 없음: {}", rawText)
            throw ServiceException(ErrorCode.AI_INVALID_JSON)
        }

        return rawText.substring(start, end + 1)
    }
}
