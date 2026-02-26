package com.back.domain.item.itemHistory.dto

import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.domain.item.itemHistory.entity.ItemHistory
import java.time.LocalDate

data class ItemAllHistoryResponse(
    val id: Long?,
    @JvmField val itemId: Long,
    @JvmField val itemName: String,
    @JvmField val categoryName: String,
    @JvmField val imgUrl: String?,
    @JvmField val startDate: LocalDate?,
    @JvmField val endDate: LocalDate?
) {
    companion object {
        /**
         * Entity -> DTO 변환을 위한 정적 팩토리 메서드
         *
         * @param itemHistory 변환할 ItemHistory 엔티티
         * @return ItemAllHistoryResponse DTO
         */
        @JvmStatic
        fun from(itemHistory: ItemHistory): ItemAllHistoryResponse {

            //엘비스 연산자를 사용하여 null일 경우 ServiceException을 보냄
            val validItem = itemHistory.item ?: throw ServiceException(
                ErrorCode.DATA_NOT_FOUND,
                "ItemHistory(${itemHistory.id})에 아이템 정보가 없습니다."
            )

            return ItemAllHistoryResponse(
                id = itemHistory.id,
                itemId = validItem.id!!,
                itemName = validItem.name!!,
                categoryName = validItem.category?.name!!,
                imgUrl = validItem.imgUrl,
                startDate = itemHistory.startDate,
                endDate = itemHistory.endDate
            )
        }

        /**
         * 여러 Entity를 한번에 변환
         *
         * @param itemHistories 변환할 ItemHistory 엔티티 리스트
         * @return ItemAllHistoryResponse DTO 리스트
         */
        @JvmStatic
        fun fromList(itemHistories: List<ItemHistory>): List<ItemAllHistoryResponse> {
            // 복잡한 Java Stream 대신 직관적인 코틀린 map 사용
            return itemHistories.map(::from)
        }
    }
}