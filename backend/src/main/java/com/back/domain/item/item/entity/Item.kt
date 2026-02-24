package com.back.domain.item.item.entity

import com.back.domain.category.category.entity.Category
import com.back.domain.item.itemHistory.entity.ItemHistory
import com.back.domain.user.user.entity.User
import com.back.global.exception.ServiceException
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "items")
class Item(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category?,

    var name: String?,

    @Column(length = 2048)
    var imgUrl: String?,

    var startDate: LocalDate?,

    var cycleDays: String?,

    var nextReplacementDate: LocalDate?,

    var isActive: Boolean = true
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    // 💡 JPA(Hibernate)가 사용할 빈 생성자 유지
    protected constructor() : this(
        user = null,
        category = null,
        name = null,
        imgUrl = null,
        startDate = null,
        cycleDays = null,
        nextReplacementDate = null,
        isActive = true
    )

    @OneToMany(mappedBy = "item", cascade = [CascadeType.ALL], orphanRemoval = true)
    var itemHistories: MutableList<ItemHistory> = mutableListOf()

    fun modifyDate(startDate: LocalDate?, nextReplacementDate: LocalDate?) {
        this.startDate = startDate
        this.nextReplacementDate = nextReplacementDate
    }

    fun validateOwner(actorUserId: Long?) {
        if (this.user?.id != actorUserId) {
            throw ServiceException("403-1", "${this.id}번 아이템에 대한 권한이 없습니다.")
        }
    }

    fun modify(
        category: Category?, name: String?, imgUrl: String?, cycleDays: String?,
        nextReplacementDate: LocalDate?, isActive: Boolean
    ) {
        this.category = category
        this.name = name
        this.imgUrl = imgUrl
        this.cycleDays = cycleDays
        this.nextReplacementDate = nextReplacementDate
        this.isActive = isActive
    }

    val lastReplacementDate: LocalDate?
        get() = itemHistories.maxOfOrNull { it.startDate ?: LocalDate.MIN }

    fun toggleActive() {
        this.isActive = !this.isActive
    }
}