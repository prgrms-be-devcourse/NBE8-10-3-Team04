package com.back.domain.item.itemHistory.repository

import com.back.domain.item.itemHistory.entity.ItemHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ItemHistoryRepository : JpaRepository<ItemHistory, Long> {

    fun findByItemIdOrderByStartDateDesc(itemId: Long): List<ItemHistory>

    @Query(
        """
        SELECT ih
        FROM ItemHistory ih
        JOIN FETCH ih.item i
        LEFT JOIN FETCH i.category c
        WHERE i.user.id = :userId
        ORDER BY ih.startDate DESC
        """
    )
    fun findByUserIdOrderByStartDateDesc(userId: Long): List<ItemHistory>

    // Kotlin에서는 Optional 대신 Nullable 타입(? 표시)을 사용
    fun findTopByItemIdAndEndDateIsNullOrderByStartDateDesc(itemId: Long): ItemHistory?

    // 특정 사용자의 카테고리별 평균 사용 기간 조회
    @Query(
        """
        SELECT ih.item.category.id as categoryId,
               ih.item.category.name as categoryName,
               AVG(TIMESTAMPDIFF(DAY, ih.startDate, ih.endDate)) as averageUsageDays
        FROM ItemHistory ih
        WHERE ih.item.user.id = :userId
          AND ih.endDate IS NOT NULL
        GROUP BY ih.item.category.id, ih.item.category.name
        ORDER BY ih.item.category.name
        """
    )
    fun findAverageUsageDaysByCategoryForUser(userId: Long): List<Map<String, Any>>

    // 특정 사용자의 가장 자주 교체한 아이템 순위 조회
    @Query(
        """
        SELECT ih.item.id as itemId,
               ih.item.name as itemName,
               ih.item.category.name as categoryName,
               COUNT(ih) as replacementCount,
               ih.item.imgUrl as imgUrl
        FROM ItemHistory ih
        WHERE ih.item.user.id = :userId
        GROUP BY ih.item.id, ih.item.name, ih.item.category.name, ih.item.imgUrl
        ORDER BY replacementCount DESC
        LIMIT :limit
        """
    )
    fun findMostReplacedItemsByUser(userId: Long, limit: Int): List<Map<String, Any>>
}