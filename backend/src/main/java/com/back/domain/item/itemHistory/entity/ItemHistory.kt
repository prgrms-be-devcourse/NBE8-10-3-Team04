package com.back.domain.item.itemHistory.entity

import com.back.domain.item.item.entity.Item
import jakarta.persistence.*
import lombok.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

@Entity
@Table(name = "item_histories")
class ItemHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null, // 주 생성자에는 id가 있지만 기본값은 null

    @ManyToOne(fetch = FetchType.LAZY)
    var item: Item?,

    var startDate: LocalDate?,

    var endDate: LocalDate? = null
) {
    constructor() : this(null, null, null, null)
    // 💡 1. 자바 테스트용: id 없이 item, startDate, endDate만 받는 생성자
    constructor(item: Item, startDate: LocalDate?, endDate: LocalDate?) : this(
        id = null, // 내부적으로 id는 null로 세팅해 줌
        item = item,
        startDate = startDate,
        endDate = endDate
    )

    // 💡 2. 기존 코드 유지: Item만 받는 생성자
    constructor(item: Item) : this(
        id = null,
        item = item,
        startDate = item.startDate, // Item이 자바 클래스면 내부적으로 getStartDate() 호출
        endDate = null
    )

    fun end(endDate: LocalDate) {
        this.endDate = endDate
    }

    // 메서드 대신 코틀린 프로퍼티(Getter) 방식 사용
    val usedDays: Long?
        get() {
            if (endDate == null || startDate == null) return null
            val days = ChronoUnit.DAYS.between(startDate, endDate)
            return maxOf(days, 0)
        }
}
