package com.back.domain.item.item.dto;

import com.back.domain.item.item.entity.Item;
import com.back.domain.item.item.util.DDayCalculator;

import java.time.LocalDate;

public record ItemCreateResponse(
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
     * 생성된 Item Entity를 Response DTO로 변환
     *
     * @param item 생성된 Item 엔티티
     * @return ItemCreateResponse DTO
     */
    public static ItemCreateResponse from(Item item) {
        return new ItemCreateResponse(
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
                item.getIsActive()
        );
    }
}