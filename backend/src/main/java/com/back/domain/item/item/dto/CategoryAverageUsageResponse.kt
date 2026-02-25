package com.back.domain.item.item.dto

data class CategoryAverageUsageResponse(
    @JvmField val categoryId: Long,
    @JvmField val categoryName: String,
    @JvmField val averageUsageDays: Double
) {
    companion object {
        /**
         * Repository 쿼리 결과(Map)를 DTO로 변환
         *
         * @param result Repository에서 반환한 Map 데이터
         * @return CategoryAverageUsageResponse DTO
         */
        @JvmStatic //
        fun from(result: Map<String, Any?>): CategoryAverageUsageResponse {
            return CategoryAverageUsageResponse(
                categoryId = (result["categoryId"] as Number).toLong(),
                categoryName = result["categoryName"] as String,
                averageUsageDays = (result["averageUsageDays"] as? Number)?.toDouble() ?: 0.0
            )
        }

        /**
         * 여러 Map을 한번에 변환
         *
         * @param results Repository에서 반환한 Map 리스트
         * @return CategoryAverageUsageResponse DTO 리스트
         */
        @JvmStatic
        fun fromList(results: List<Map<String, Any?>>): List<CategoryAverageUsageResponse> {
            return results.map(::from)
        }
    }
}