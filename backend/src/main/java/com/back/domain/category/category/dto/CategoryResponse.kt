package com.back.domain.category.category.dto

import com.back.domain.category.category.entity.Category


data class CategoryResponse(
    val id: Long?,
    val name: String,
    val itemCount: Long
) {
    companion object {
        fun from(category: Category): CategoryResponse =
            CategoryResponse(
                category.id,
                category.name,
                0L
            )

        fun of(category: Category, itemCount: Long): CategoryResponse =
            CategoryResponse(
                category.id,
                category.name,
                itemCount
            )

        fun fromList(categories: List<Category>): List<CategoryResponse> =
            categories.map(::from)
    }
}
