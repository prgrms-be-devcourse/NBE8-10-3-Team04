package com.back.domain.item.item.dto;

import java.util.List;
import java.util.Map;

public record MostReplacedItemResponse(
        Long itemId,
        String itemName,
        String categoryName,
        Long replacementCount,
        String imgUrl
) {
    /**
     * Repository 쿼리 결과(Map)를 DTO로 변환
     *
     * @param result Repository에서 반환한 Map 데이터
     * @return MostReplacedItemResponse DTO
     */
    public static MostReplacedItemResponse from(Map<String, Object> result) {
        return new MostReplacedItemResponse(
                ((Number) result.get("itemId")).longValue(),
                (String) result.get("itemName"),
                (String) result.get("categoryName"),
                ((Number) result.get("replacementCount")).longValue(),
                (String) result.get("imgUrl")
        );
    }

    /**
     * 여러 Map을 한번에 변환
     *
     * @param results Repository에서 반환한 Map 리스트
     * @return MostReplacedItemResponse DTO 리스트
     */
    public static List<MostReplacedItemResponse> fromList(List<Map<String, Object>> results) {
        return results.stream()
                .map(MostReplacedItemResponse::from)
                .toList();
    }
}