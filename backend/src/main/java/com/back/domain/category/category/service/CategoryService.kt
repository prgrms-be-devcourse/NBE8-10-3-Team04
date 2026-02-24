package com.back.domain.category.category.service

import com.back.domain.category.category.dto.CategoryResponse
import com.back.domain.category.category.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) {
    @Transactional(readOnly = true)
    fun findAllWithItemCount(userId: Long): List<CategoryResponse> =
        categoryRepository.findAllWithItemCount(userId)
}
