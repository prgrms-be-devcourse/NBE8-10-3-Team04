package com.back.domain.item.item.dto

import com.back.domain.item.item.entity.Item
import com.back.domain.item.item.util.DDayCalculator
import java.time.LocalDate

@JvmRecord
data class ItemCreateResponse(
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
         * 생성된 Item Entity를 Response DTO로 변환
         *
         * @param item 생성된 Item 엔티티
         * @return ItemCreateResponse DTO
         */
        @JvmStatic
        fun from(item: Item): ItemCreateResponse {
            return ItemCreateResponse(
                id = item.id,
                userId = item.user?.id,
                categoryId = item.category?.id,
                categoryName = item.category?.name,
                name = item.name,
                imgUrl = item.imgUrl,
                startDate = item.startDate,
                cycleDays = item.cycleDays,
                nextReplacementDate = item.nextReplacementDate,
                dDay = DDayCalculator.calculate(item.nextReplacementDate),
                isActive = item.isActive
            )
        }
    }
}