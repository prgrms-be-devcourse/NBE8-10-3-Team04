package com.back.domain.item.itemHistory.service

import com.back.domain.item.item.entity.Item
import com.back.domain.item.item.repository.ItemRepository
import com.back.domain.item.itemHistory.dto.ItemAllHistoryResponse
import com.back.domain.item.itemHistory.dto.ItemHistoryResponse
import com.back.domain.item.itemHistory.entity.ItemHistory
import com.back.domain.item.itemHistory.repository.ItemHistoryRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class ItemHistoryService(
    private val itemHistoryRepository: ItemHistoryRepository,
    private val itemRepository: ItemRepository
) {

    @Transactional
    fun createItemHistory(item: Item) {
        val itemHistory = ItemHistory(item)
        itemHistoryRepository.save(itemHistory)
    }

    @Transactional(readOnly = true)
    fun getItemHistories(itemId: Long, userId: Long): List<ItemHistoryResponse> {
        itemRepository.findByIdAndUserId(itemId, userId)
            .orElseThrow { ServiceException(ErrorCode.ITEM_NOT_FOUND_OR_NO_PERMISSION) }

        val histories = itemHistoryRepository.findByItemIdOrderByStartDateDesc(itemId)
        return ItemHistoryResponse.fromList(histories)
    }

    @Transactional(readOnly = true)
    fun getAllItemHistories(userId: Long): List<ItemAllHistoryResponse> {
        val histories = itemHistoryRepository.findByUserIdOrderByStartDateDesc(userId)
        return ItemAllHistoryResponse.fromList(histories)
    }

    @Transactional
    fun endHistory(itemId: Long, endDate: LocalDate) {
        // Repository가 Optional이 아닌 객체(Nullable)를 반환하므로 엘비스 연산자로 null 체크
        val ongoing = itemHistoryRepository.findTopByItemIdAndEndDateIsNullOrderByStartDateDesc(itemId)
            ?: throw ServiceException(ErrorCode.ONGOING_HISTORY_NOT_FOUND)

        ongoing.end(endDate)
    }
}