package com.back.domain.item.item.dto

@JvmRecord
data class ItemCycleRecommendResponse(
    @JvmField val cycleValue: Int,
    @JvmField val cycleUnit: String? // "d", "m", "y"
)
