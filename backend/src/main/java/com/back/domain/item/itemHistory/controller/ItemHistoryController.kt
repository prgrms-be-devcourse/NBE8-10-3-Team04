package com.back.domain.item.itemHistory.controller

import com.back.domain.item.itemHistory.dto.ItemAllHistoryResponse
import com.back.domain.item.itemHistory.dto.ItemHistoryResponse
import com.back.domain.item.itemHistory.service.ItemHistoryService
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/items")
@Tag(name = "ItemHistoryController", description = "아이템 이력 컨트롤러")
class ItemHistoryController(
    private val itemHistoryService: ItemHistoryService,
    private val rq: Rq
) {

    //단일 표현식 함수로 변경
    @GetMapping("/histories")
    @Operation(summary = "전체 아이템 이력 조회", description = "전체 아이템의 이력을 일괄 조회합니다.")
    fun getAllItemHistories(): RsData<List<ItemAllHistoryResponse>> =
        RsData(
            "200-1",
            "전체 아이템 이력 조회 성공",
            itemHistoryService.getAllItemHistories(rq.memberId)
        )


    @GetMapping("/{itemId}/histories")
    @Operation(summary = "특정 아이템의 교체 이력 조회", description = "특정 아이템의 이력을 조회합니다.")
    fun getItemHistories(@PathVariable itemId: Long): RsData<List<ItemHistoryResponse>> =
        RsData(
            "200-1",
            "아이템 이력 조회 성공",
            itemHistoryService.getItemHistories(itemId, rq.memberId)
        )
}
