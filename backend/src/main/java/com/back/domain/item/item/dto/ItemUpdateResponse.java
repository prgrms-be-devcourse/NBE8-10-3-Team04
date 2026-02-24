package com.back.domain.item.item.dto;

import com.back.domain.item.item.entity.Item;
import com.back.domain.item.item.util.DDayCalculator;

import java.time.LocalDate;

public record ItemUpdateResponse(
        Long id,
        Long userId,
        Long categoryId,
        String categoryName,
        String name,
        String imgUrl,
        LocalDate startDate,
        String cycleDays,
        LocalDate nextReplacementDate,
        Long dDay,
        Boolean isActive
) {
    /**
     * 수정된 Item Entity를 Response DTO로 변환
     *
     * @param item 수정된 Item 엔티티
     * @return ItemUpdateResponse DTO
     */
    public static ItemUpdateResponse from(Item item) {
        return new ItemUpdateResponse(
                item.getId(),
                item.getUser().getId(),
                item.getCategory() == null ? null : item.getCategory().getId(),
                item.getCategory() == null ? null : item.getCategory().getName(),
                item.getName(),
                item.getImgUrl(),
                item.getStartDate(),
                item.getCycleDays(),
                item.getNextReplacementDate(),
                DDayCalculator.calculate(item.getNextReplacementDate()),
                item.isActive()
        );
    }
}