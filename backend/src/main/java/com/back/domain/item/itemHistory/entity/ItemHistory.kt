package com.back.domain.item.itemHistory.entity

import com.back.domain.item.item.entity.Item
import jakarta.persistence.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Entity
@Table(name = "item_histories")
class ItemHistory(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    var item: Item, // Non-null 변경

    @Column(nullable = false)
    var startDate: LocalDate, // Non-null로 변경

    var endDate: LocalDate? // 종료일은 null일 수 있으므로 Nullable 유지
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    //  JPA용 빈 생성자는 플러그인이 알아서 만들어주므로 완전히 삭제

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
            // startDate가 Non-null이 되었으므로 endDate만 null 체크
            if (endDate == null) return null
            val days = ChronoUnit.DAYS.between(startDate, endDate)
            return maxOf(days, 0)
        }
}
