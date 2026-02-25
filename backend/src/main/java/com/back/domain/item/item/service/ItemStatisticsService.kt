package com.back.domain.item.item.service

import com.back.domain.item.item.dto.CategoryAverageUsageResponse
import com.back.domain.item.item.dto.MostReplacedItemResponse
import com.back.domain.item.itemHistory.repository.ItemHistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 아이템 통계 정보를 제공하는 서비스
 * - 카테고리별 평균 사용 기간
 * - 가장 자주 교체한 아이템 순위
 */
@Service
class ItemStatisticsService(
    private val itemHistoryRepository: ItemHistoryRepository
) {

    /**
     * 특정 사용자의 카테고리별 평균 사용 기간 조회
     *
     * @param userId 사용자 ID
     * @return 카테고리별 평균 사용 기간 목록
     */
    @Transactional(readOnly = true)
    fun getCategoryAverageUsage(userId: Long): List<CategoryAverageUsageResponse> {
        // Repository에서 카테고리별 평균 사용 기간을 조회
        val rawResults = itemHistoryRepository.findAverageUsageDaysByCategoryForUser(userId)

        // 결과를 DTO로 변환
        return CategoryAverageUsageResponse.fromList(rawResults)
    }

    /**
     * 특정 사용자의 가장 자주 교체한 아이템 순위 조회
     *
     * @param userId 사용자 ID
     * @param limit 조회할 최대 개수
     * @return 가장 자주 교체한 아이템 목록
     */
    @Transactional(readOnly = true)
    fun getMostReplacedItems(userId: Long, limit: Int): List<MostReplacedItemResponse> {
        val rawResults = itemHistoryRepository.findMostReplacedItemsByUser(userId, limit)

        return MostReplacedItemResponse.fromList(rawResults)
    }
}