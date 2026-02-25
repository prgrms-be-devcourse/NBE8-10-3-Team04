package com.back.domain.item.item.dto

import com.back.domain.item.item.entity.Item
import com.back.domain.item.item.util.DDayCalculator.calculate
import java.time.LocalDate

// Kotlin의 data class는 불변 객체의 특성을 기본적으로 제공하므로 @JvmRecord 애노테이션을 제거
// 엔티티 설계상 isActive는 기본값을 가지는 Non-null 타입이므로 Boolean? 대신 Boolean으로 수정
data class ItemSummaryResponse(
    val id: Long?,
    val name: String?,
    val categoryName: String?,
    val nextReplacementDate: LocalDate?,
    val lastReplacementDate: LocalDate?,
    val imgUrl: String?,
    val dDay: Long?,
    val isActive: Boolean
) {
    companion object {
        // Entity -> DTO 변환을 위한 정적 팩토리 메서드
        fun from(item: Item): ItemSummaryResponse = ItemSummaryResponse(
            // Named Arguments를 사용
            id = item.id,
            name = item.name,
            // 자바식의 if-else null 체크 대신 코틀린의 안전 호출 연산자(?.)를 사용
            categoryName = item.category?.name,
            nextReplacementDate = item.nextReplacementDate,
            lastReplacementDate = item.lastReplacementDate,
            imgUrl = item.imgUrl,
            dDay = calculate(item.nextReplacementDate),
            isActive = item.isActive
        )

        // 여러 Entity를 한번에 변환하는 유틸리티 메서드
        // MutableList<Item?> 대신 읽기 전용 List<Item>를 사용하여 불변성을 강조
        fun fromList(items: List<Item>): List<ItemSummaryResponse> =
            // Java의 stream().map().toList() 대신 코틀린의 내장 컬렉션 확장 함수인 map을 사용하여 코드를 단축
            items.map { from(it) }
    }
}