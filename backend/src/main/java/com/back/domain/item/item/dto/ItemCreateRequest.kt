package com.back.domain.item.item.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

@JvmRecord
data class ItemCreateRequest(
    // 1. @field: 를 반드시 붙여야 합니다.
    @field:NotNull
    @JvmField val categoryId: Long?,

    // 2. @field:NotBlank 로 수정
    @field:NotBlank
    @JvmField val name: String?,

    @JvmField val imgUrl: String?,
    @JvmField val image: MultipartFile?,
    val startDate: LocalDate?,

    // 3. 여기도 @field:NotBlank
    @field:NotBlank
    @JvmField val cycleDays: String?
) {
    fun resolvedStartDate(): LocalDate {
        return startDate ?: LocalDate.now()
    }
}