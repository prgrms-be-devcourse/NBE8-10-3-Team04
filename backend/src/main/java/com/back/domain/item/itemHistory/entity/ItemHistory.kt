package com.back.domain.item.itemHistory.entity

import com.back.domain.item.item.entity.Item
import jakarta.persistence.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Entity
@Table(name = "item_histories")
class ItemHistory(

    @ManyToOne(fetch = FetchType.LAZY)
    var item: Item?,

    var startDate: LocalDate?,

    var endDate: LocalDate?
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    constructor() : this(
        item = null,
        startDate = LocalDate.now(),
        endDate = null
    )
    constructor(item: Item) : this(
        item = item,
        startDate = item.startDate,
        endDate = null
    )

    fun end(endDate: LocalDate) {
        this.endDate = endDate
    }

    val usedDays: Long?
        get() {
            if (endDate == null || startDate == null) return null
            val days = ChronoUnit.DAYS.between(startDate, endDate)
            return maxOf(days, 0)
        }
}
