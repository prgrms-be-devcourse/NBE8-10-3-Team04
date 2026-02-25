package com.back.domain.item.item.dto

import com.back.domain.item.item.entity.Item
import com.back.domain.item.item.util.DDayCalculator.calculate
import java.time.LocalDate

data class ItemResponse(
    val id: Long?,
    val userId: Long?,
    val categoryId: Long?,
    val categoryName: String?,
    val name: String?,
    val imgUrl: String?,
    val startDate: LocalDate?,
    val cycleDays: String?,
    val nextReplacementDate: LocalDate?,
    // Entity의 isActive가 기본값을 가지는 Non-null Boolean이므로 DTO에서도 Boolean
    val isActive: Boolean,
    val dDay: Long?,
    val lastReplacementDate: LocalDate?
) {
    companion object {
        /**
         * Entity -> DTO 변환을 위한 정적 팩토리 메서드
         */
        fun from(item: Item): ItemResponse {
            // Named Argument를 사용하여 각 필드가 어떤 값을 받는지 명확히 하여 가독성 높임
            return ItemResponse(
                id = item.id,
                //  !! 대신 ?.을 사용하여 NPE 발생 위험 줄임
                userId = item.user?.id,
                // if-else 문을 지우고 ?.로 대체
                categoryId = item.category?.id,
                categoryName = item.category?.name,
                name = item.name,
                imgUrl = item.imgUrl,
                startDate = item.startDate,
                cycleDays = item.cycleDays,
                nextReplacementDate = item.nextReplacementDate,
                isActive = item.isActive,
                dDay = calculate(item.nextReplacementDate),
                lastReplacementDate = item.lastReplacementDate
            )
        }

        // 여러 Entity를 한번에 변환하는 유틸리티 메서드
        // MutableList 대신 불변 타입인 List를 사용하고, Nullable(Item?, ItemResponse?) 요소를 Non-null로 변경
        fun fromList(items: List<Item>): List<ItemResponse> {
            // Java의 Stream API 대신 코틀린의 컬렉션 확장 함수(map)를 사용
            return items.map { from(it) }
        }
    }
}