package com.back.domain.item.item.dto;

import com.back.domain.item.item.entity.Item;
import com.back.domain.item.item.util.DDayCalculator;

import java.time.LocalDate;

public record ItemReplaceResponse(
        Long id,
        Long userId,
        Long categoryId,
        String categoryName,
        String name,
        String imgUrl,
        LocalDate startDate,
        String cycleDays,
        LocalDate nextReplacementDate,
        Long dDay
) {
    /**
     * 교체된 Item Entity를 Response DTO로 변환
     *
     * @param item 교체된 Item 엔티티
     * @return ItemReplaceResponse DTO
     */
    public static ItemReplaceResponse from(Item item) {
        return new ItemReplaceResponse(
                item.getId(),
                item.getUser().getId(),
                item.getCategory() == null ? null : item.getCategory().getId(),
                item.getCategory() == null ? null : item.getCategory().getName(),
                item.getName(),
                item.getImgUrl(),
                item.getStartDate(),
                item.getCycleDays(),
                item.getNextReplacementDate(),
                DDayCalculator.calculate(item.getNextReplacementDate())
        );
    }
}