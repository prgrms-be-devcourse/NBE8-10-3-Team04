package com.back.domain.item.item.dto;

public record ItemCycleRecommendResponse(
        int cycleValue,
        String cycleUnit // "d", "m", "y"
) {
}
