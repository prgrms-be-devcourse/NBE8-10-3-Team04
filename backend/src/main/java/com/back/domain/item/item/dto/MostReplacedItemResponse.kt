package com.back.domain.item.item.dto


data class MostReplacedItemResponse(
    @JvmField val itemId: Long,
    @JvmField val itemName: String,
    @JvmField val categoryName: String,
    @JvmField val replacementCount: Long,
    @JvmField val imgUrl: String?
) {
    companion object {
        /**
         * Repository 쿼리 결과(Map)를 DTO로 변환
         *
         * @param result Repository에서 반환한 Map 데이터
         * @return MostReplacedItemResponse DTO
         */
        @JvmStatic
        fun from(result: Map<String, Any?>): MostReplacedItemResponse {
            return MostReplacedItemResponse(
                itemId = (result["itemId"] as Number).toLong(),
                itemName = result["itemName"] as String,
                categoryName = result["categoryName"] as String,
                replacementCount = (result["replacementCount"] as Number).toLong(),
                imgUrl = result["imgUrl"] as? String
            )
        }

        /**
         * 여러 Map을 한번에 변환
         *
         * @param results Repository에서 반환한 Map 리스트
         * @return MostReplacedItemResponse DTO 리스트
         */
        @JvmStatic
        fun fromList(results: List<Map<String, Any?>>): List<MostReplacedItemResponse> {
            return results.map(::from)
        }
    }
}