package com.back.domain.item.item.entity

import com.back.domain.category.category.entity.Category
import com.back.domain.item.itemHistory.entity.ItemHistory
import com.back.domain.user.user.entity.User
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "items")
class Item(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User, // Non-null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category, // Non-null

    @Column(nullable = false)
    var name: String, // Non-null

    @Column(length = 2048)
    var imgUrl: String?, // 이미지는 없을 수도 있으므로 유지 (Nullable이 맞음)

    @Column(nullable = false)
    var startDate: LocalDate, // Non-null

    @Column(nullable = false)
    var cycleDays: String, // Non-null

    @Column(nullable = false)
    var nextReplacementDate: LocalDate, // Non-null

    var isActive: Boolean = true
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null // DB에서 생성되므로 Nullable 유지

    // JPA를 위한 기본 생성자는 kotlin-jpa 플러그인이 알아서 만들어주므로 완전히 삭제

    @OneToMany(mappedBy = "item", cascade = [CascadeType.ALL], orphanRemoval = true)
    var itemHistories: MutableList<ItemHistory> = mutableListOf()

    // 메서드 파라미터들도 명확하게 Non-null로 변경
    fun modifyDate(startDate: LocalDate, nextReplacementDate: LocalDate) {
        this.startDate = startDate
        this.nextReplacementDate = nextReplacementDate
    }

    fun validateOwner(actorUserId: Long) {
        // user가 Non-null이 되었으므로 안전하게 접근 가능
        if (this.user.id != actorUserId) {
            throw ServiceException(ErrorCode.ITEM_NOT_FOUND_OR_NO_PERMISSION)
        }
    }

    fun modify(
        category: Category,
        name: String,
        imgUrl: String?, // 이미지는 수정 시에도 지울 수 있거나 없을 수 있으니 Nullable
        cycleDays: String,
        nextReplacementDate: LocalDate,
        isActive: Boolean
    ) {
        this.category = category
        this.name = name
        this.imgUrl = imgUrl
        this.cycleDays = cycleDays
        this.nextReplacementDate = nextReplacementDate
        this.isActive = isActive
    }

    val lastReplacementDate: LocalDate?
        get() = itemHistories.maxOfOrNull { it.startDate } // startDate도 Non-null이라면 좀 더 깔끔

    fun toggleActive() {
        this.isActive = !this.isActive
    }
}