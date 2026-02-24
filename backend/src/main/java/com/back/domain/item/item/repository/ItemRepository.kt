package com.back.domain.item.item.repository

import com.back.domain.item.item.entity.Item
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface ItemRepository : JpaRepository<Item, Long> {
    // 목록조회
    fun findAllByUserIdOrderByNextReplacementDateAsc(userId: Long): List<Item>

    // 단건조회
    //TODO Item Service 코틀린전환후 Optional 객체로 감쌀 필요 없이 그냥 Item?을 반환하도록 변경
    fun findByIdAndUserId(id: Long, userId: Long): Optional<Item>

    // 카테고리별목록조회
    fun findAllByUserIdAndCategoryId(userId: Long, categoryId: Long): List<Item>

    // 특정 날짜가 교체 예정일이고 활성화된 아이템 조회
    fun findAllByNextReplacementDateAndIsActive(
        nextReplacementDate: LocalDate,
        isActive: Boolean
    ): List<Item>
}
