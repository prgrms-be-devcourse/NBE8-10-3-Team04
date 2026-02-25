package com.back.domain.item.item.dto

import com.back.domain.item.item.entity.Item
import com.back.domain.item.item.util.DDayCalculator.calculate
import java.time.LocalDate

data class ItemUpdateResponse(
    val id: Long?,
    val userId: Long?,
    val categoryId: Long?,
    val categoryName: String?,
    val name: String?,
    val imgUrl: String?,
    val startDate: LocalDate?,
    val cycleDays: String?,
    val nextReplacementDate: LocalDate?,
    val dDay: Long?,
    val isActive: Boolean?
) {
    companion object {
        /**
         * 수정된 Item Entity를 Response DTO로 변환
         *
         * @param item 수정된 Item 엔티티
         * @return ItemUpdateResponse DTO
         */
        fun from(item: Item) = ItemUpdateResponse(
            id = item.id,
            userId = item.user?.id, // 단언 연산자(!!) 대신 안전한 호출(?.) 사용
            categoryId = item.category?.id, // if-else 널 체크를 Safe Call(?.)로 간결하게 처리
            categoryName = item.category?.name,
            name = item.name,
            imgUrl = item.imgUrl,
            startDate = item.startDate,
            cycleDays = item.cycleDays,
            nextReplacementDate = item.nextReplacementDate,
            dDay = calculate(item.nextReplacementDate),
            isActive = item.isActive
        )
    }
}