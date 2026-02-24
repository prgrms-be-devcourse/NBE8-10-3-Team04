package com.back.domain.category.category.controller

import com.back.domain.category.category.dto.CategoryResponse
import com.back.domain.category.category.service.CategoryService
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "CategoryController", description = "카테고리 컨트롤러")
class CategoryController(
    private val categoryService: CategoryService,
    private val rq: Rq
) {

    @GetMapping
    @Operation(summary = "카테고리 목록 조회 (사용자별 아이템 개수 포함)")
    fun getCategories(): RsData<List<CategoryResponse>> {
        val userId = rq.memberId

        val categories = categoryService.findAllWithItemCount(userId)

        return RsData(
            "200-1",
            "카테고리 목록 조회 성공",
            categories
        )
    }
}
