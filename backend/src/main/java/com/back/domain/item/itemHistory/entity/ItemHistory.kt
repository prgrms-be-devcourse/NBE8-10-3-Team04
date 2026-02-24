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
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    var item: Item?,

    var startDate: LocalDate?,

    var endDate: LocalDate? = null
) {
    constructor() : this(null, null, null, null)
    constructor(item: Item, startDate: LocalDate?, endDate: LocalDate?) : this(
        id = null,
        item = item,
        startDate = startDate,
        endDate = endDate
    )

    constructor(item: Item) : this(
        id = null,
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
