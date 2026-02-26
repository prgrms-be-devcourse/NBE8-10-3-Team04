package com.back.domain.item.itemHistory.service;

import com.back.domain.item.item.entity.Item;
import com.back.domain.item.item.repository.ItemRepository;
import com.back.domain.item.itemHistory.dto.ItemAllHistoryResponse;
import com.back.domain.item.itemHistory.dto.ItemHistoryResponse;
import com.back.domain.item.itemHistory.entity.ItemHistory;
import com.back.domain.item.itemHistory.repository.ItemHistoryRepository;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemHistoryService {
    private final ItemHistoryRepository itemHistoryRepository;
    private final ItemRepository itemRepository;

    @Transactional
    public void createItemHistory(Item item) {
        ItemHistory itemHistory = new ItemHistory(item);
        itemHistoryRepository.save(itemHistory);
    }

    @Transactional(readOnly = true)
    public List<ItemHistoryResponse> getItemHistories(Long itemId, Long userId) {
        itemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.ITEM_NOT_FOUND_OR_NO_PERMISSION));
        List<ItemHistory> histories = itemHistoryRepository.findByItemIdOrderByStartDateDesc(itemId);
        return ItemHistoryResponse.fromList(histories);
    }

    @Transactional(readOnly = true)
    public List<ItemAllHistoryResponse> getAllItemHistories(Long userId) {
        List<ItemHistory> histories = itemHistoryRepository.findByUserIdOrderByStartDateDesc(userId);
        return ItemAllHistoryResponse.fromList(histories);
    }

    @Transactional
    public void endHistory(Long itemId, LocalDate endDate) {
        // Repository가 Optional이 아닌 객체(Nullable)를 반환하므로 null 체크로 변경
        ItemHistory ongoing = itemHistoryRepository.findTopByItemIdAndEndDateIsNullOrderByStartDateDesc(itemId);

        if (ongoing == null) {
            throw new ServiceException(ErrorCode.ONGOING_HISTORY_NOT_FOUND);
        }

        ongoing.end(endDate);
    }
}
