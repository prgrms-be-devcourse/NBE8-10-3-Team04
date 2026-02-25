package com.back.domain.item.itemHistory.dto

import com.back.domain.item.itemHistory.entity.ItemHistory
import java.time.LocalDate


data class ItemHistoryResponse(
    val id: Long,
    @JvmField val itemId: Long,
    @JvmField val startDate: LocalDate?,
    @JvmField val endDate: LocalDate?
) {
    companion object {
        /**
         * Entity -> DTO 변환을 위한 정적 팩토리 메서드
         *
         * @param itemHistory 변환할 ItemHistory 엔티티
         * @return ItemHistoryResponse DTO
         */
        @JvmStatic
        fun from(itemHistory: ItemHistory): ItemHistoryResponse {
            return ItemHistoryResponse(
                id = itemHistory.id!!,
                itemId = itemHistory.item?.id!!,
                startDate = itemHistory.startDate,
                endDate = itemHistory.endDate
            )
        }

        /**
         * 여러 Entity를 한번에 변환
         *
         * @param itemHistories 변환할 ItemHistory 엔티티 리스트
         * @return ItemHistoryResponse DTO 리스트
         */
        @JvmStatic
        fun fromList(itemHistories: List<ItemHistory>): List<ItemHistoryResponse> {
            return itemHistories.map(::from)
        }
    }
}