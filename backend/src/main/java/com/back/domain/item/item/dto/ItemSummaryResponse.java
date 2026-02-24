package com.back.domain.item.item.dto;

import com.back.domain.item.item.entity.Item;
import com.back.domain.item.item.util.DDayCalculator;

import java.time.LocalDate;
import java.util.List;

public record ItemSummaryResponse(
        Long id,
        String name,
        String categoryName,
        LocalDate nextReplacementDate,
        LocalDate lastReplacementDate,
        String imgUrl,
        Long dDay,
        Boolean isActive
) {
    /**
     * Entity -> DTO 변환을 위한 정적 팩토리 메서드
     *
     * @param item 변환할 Item 엔티티
     * @return ItemSummaryResponse DTO
     */
    public static ItemSummaryResponse from(Item item) {
        return new ItemSummaryResponse(
                item.getId(),
                item.getName(),
                item.getCategory() == null ? null : item.getCategory().getName(),
                item.getNextReplacementDate(),
                item.getLastReplacementDate(),
                item.getImgUrl(),
                DDayCalculator.calculate(item.getNextReplacementDate()),
                item.getIsActive()
        );
    }

    /**
     * 여러 Entity를 한번에 변환하는 유틸리티 메서드
     *
     * @param items 변환할 Item 엔티티 리스트
     * @return ItemSummaryResponse DTO 리스트
     */
    public static List<ItemSummaryResponse> fromList(List<Item> items) {
        return items.stream()
                .map(ItemSummaryResponse::from)
                .toList();
    }
}