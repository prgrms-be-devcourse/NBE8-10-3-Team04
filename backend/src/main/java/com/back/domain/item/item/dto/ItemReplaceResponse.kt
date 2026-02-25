package com.back.domain.item.item.dto

import com.back.domain.item.item.entity.Item
import com.back.domain.item.item.util.DDayCalculator
import java.time.LocalDate

data class ItemReplaceResponse(
    val id: Long,
    val userId: Long,
    val categoryId: Long?,
    val categoryName: String?,
    val name: String,
    val imgUrl: String?,
    val startDate: LocalDate?,
    val cycleDays: String?,
    val nextReplacementDate: LocalDate?,
    val dDay: Long?
) {
    companion object {
        /**
         * 교체된 Item Entity를 Response DTO로 변환
         *
         * @param item 교체된 Item 엔티티
         * @return ItemReplaceResponse DTO
         */
        @JvmStatic
        fun from(item: Item): ItemReplaceResponse {
            return ItemReplaceResponse(
                id = item.id!!,
                item.user!!.id,
                if (item.category == null) null else item.category!!.id,
                if (item.category == null) null else item.category!!.name,
                name = item.name ?: "",
                item.imgUrl,
                item.startDate,
                item.cycleDays,
                item.nextReplacementDate,
                DDayCalculator.calculate(item.nextReplacementDate)
            )
        }
    }
}