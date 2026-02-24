package com.back.domain.item.item.controller

import com.back.domain.item.item.dto.*
import com.back.domain.item.item.entity.Item
import com.back.domain.item.item.service.ItemRecommendationService
import com.back.domain.item.item.service.ItemService
import com.back.domain.item.item.service.ItemStatisticsService
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.jpa.domain.AbstractPersistable_.id
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/items")
@Validated
@Tag(name = "ItemController", description = "아이템 컨트롤러")
class ItemController (
    private val itemService: ItemService,
    private val rq: Rq,
    private val itemStatisticsService: ItemStatisticsService,
    private val itemRecommendationService: ItemRecommendationService
) {
    // == CRUD ==
    @DeleteMapping("/{itemId}")
    @Operation(summary = "아이템 삭제")
    fun deleteItem(@PathVariable itemId: Long): RsData<Void> {
        val userId = rq.memberId
        itemService.deleteItem(userId, itemId)

        return RsData("200", "아이템 삭제 성공")
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]) //Swagger UI에 파일선택버튼 추가
    @Operation(summary = "아이템 등록")
    fun createItem(
        @ModelAttribute @Valid request: ItemCreateRequest
    ): RsData<ItemCreateResponse> {
        val userId = rq.memberId
        val item = itemService.createItem(userId, request)

        return RsData(
            "200",
            "아이템 등록 성공",
            ItemCreateResponse.from(item)
        )
    }

    @GetMapping
    @Operation(summary = "아이템 목록 조회")
    fun getItems(
        @RequestParam(required = false) categoryId: Long?
    ): RsData<List<ItemSummaryResponse>> {
        val userId = rq.memberId
        val items = if (categoryId != null) {
            itemService.findAllByUserIdAndCategoryId(userId, categoryId)
        } else {
            itemService.findAllByUserIdOrderByNextReplacementDateAsc(userId)
        }

        return RsData(
            "200",
            "아이템 목록 조회 성공",
            ItemSummaryResponse.fromList(items)
        )
    }

    @GetMapping("/{itemId}")
    @Operation(summary = "아이템 단건 조회")
    fun getItem(@PathVariable itemId: Long): RsData<ItemResponse> {
        val userId = rq.memberId
        val item = itemService.findByIdAndUserId(itemId, userId)

        return RsData(
            "200",
            "아이템 단건 조회 성공",
            ItemResponse.from(item)
        )
    }

    @PutMapping("/{id}")
    @Operation(summary = "아이템 수정")
    fun modifyItem(
        @PathVariable("id") itemId: Long,
        @ModelAttribute @Valid request: ItemUpdateRequest
    ): RsData<ItemUpdateResponse> {
        val userId = rq.memberId
        val item = itemService.modify(userId, itemId, request)

        return RsData(
            "200",
            "아이템 수정 성공",
            ItemUpdateResponse.from(item)
        )
    }

    @PutMapping("/{id}/replace")
    @Operation(summary = "아이템 교체")
    fun replaceItem(@PathVariable("id") itemId: Long): RsData<ItemReplaceResponse> {
        val userId = rq.memberId
        val item = itemService.replaceItem(userId, itemId)

        return RsData(
            "200",
            "아이템 교체 처리 성공",
            ItemReplaceResponse.from(item)
        )
    }

    @PutMapping("/{id}/toggle-active")
    @Operation(summary = "아이템 활성화/비활성화 토글")
    fun toggleItemActive(@PathVariable("id") itemId: Long): RsData<ItemUpdateResponse> {
        val userId = rq.memberId
        val item = itemService.toggleActive(userId, itemId)

        return RsData(
            "200",
            "아이템 활성화 상태 변경 성공",
            ItemUpdateResponse.from(item)
        )
    }

    @GetMapping("/statistics/category-average")
    @Operation(summary = "카테고리별 평균 사용 기간 조회")
    fun getCategoryAverageUsage(): RsData<List<CategoryAverageUsageResponse>> {
        val userId = rq.memberId
        val data = itemStatisticsService.getCategoryAverageUsage(userId)

        return RsData("200", "카테고리별 평균 사용 기간 조회 성공", data)
    }

    @GetMapping("/statistics/most-replaced")
    @Operation(summary = "가장 자주 교체한 아이템 순위 조회")
    fun getMostReplacedItems(
        @RequestParam(defaultValue = "10") limit: Int
    ): RsData<List<MostReplacedItemResponse>> {
        val userId = rq.memberId
        val data = itemStatisticsService.getMostReplacedItems(userId, limit)

        return RsData("200", "가장 자주 교체한 아이템 순위 조회 성공", data)
    }

    // == AI 추천 ==
    @GetMapping("/cycle-recommend")
    @Operation(summary = "AI에게 아이템 주기 추천받기")
    fun getItemCycleRecommend(@RequestParam name: String): RsData<ItemCycleRecommendResponse> {
        val recommendation = itemRecommendationService.getItemCycleRecommend(name)
        return RsData(
            "200",
            "추천 주기 조회 완료",
            recommendation
        )
    }
}