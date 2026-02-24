package com.back.domain.item.item.dto;

import com.back.domain.item.item.entity.Item;
import com.back.domain.item.item.util.DDayCalculator;

import java.time.LocalDate;
import java.util.List;

public record ItemResponse(
        Long id,
        Long userId,
        Long categoryId,
        String categoryName,
        String name,
        String imgUrl,
        LocalDate startDate,
        String cycleDays,
        LocalDate nextReplacementDate,
        Boolean isActive,
        Long dDay,
        LocalDate lastReplacementDate
) {
    /**
     * Entity -> DTO 변환을 위한 정적 팩토리 메서드
     *
     * @param item 변환할 Item 엔티티
     * @return ItemResponse DTO
     */
    public static ItemResponse from(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getUser().getId(),
                item.getCategory() == null ? null : item.getCategory().getId(),
                item.getCategory() == null ? null : item.getCategory().getName(),
                item.getName(),
                item.getImgUrl(),
                item.getStartDate(),
                item.getCycleDays(),
                item.getNextReplacementDate(),
                item.getIsActive(),
                DDayCalculator.calculate(item.getNextReplacementDate()),
                item.getLastReplacementDate()
        );
    }

    /**
     * 여러 Entity를 한번에 변환하는 유틸리티 메서드
     *
     * @param items 변환할 Item 엔티티 리스트
     * @return ItemResponse DTO 리스트
     */
    public static List<ItemResponse> fromList(List<Item> items) {
        return items.stream()
                .map(ItemResponse::from)
                .toList();
    }
}