package com.back.domain.item.item.dto;

import java.util.List;
import java.util.Map;

public record CategoryAverageUsageResponse(
        Long categoryId,
        String categoryName,
        Double averageUsageDays
) {
    /**
     * Repository 쿼리 결과(Map)를 DTO로 변환
     *
     * @param result Repository에서 반환한 Map 데이터
     * @return CategoryAverageUsageResponse DTO
     */
    public static CategoryAverageUsageResponse from(Map<String, Object> result) {
        return new CategoryAverageUsageResponse(
                ((Number) result.get("categoryId")).longValue(),
                (String) result.get("categoryName"),
                result.get("averageUsageDays") != null
                        ? ((Number) result.get("averageUsageDays")).doubleValue()
                        : 0.0
        );
    }

    /**
     * 여러 Map을 한번에 변환
     *
     * @param results Repository에서 반환한 Map 리스트
     * @return CategoryAverageUsageResponse DTO 리스트
     */
    public static List<CategoryAverageUsageResponse> fromList(List<Map<String, Object>> results) {
        return results.stream()
                .map(CategoryAverageUsageResponse::from)
                .toList();
    }
}