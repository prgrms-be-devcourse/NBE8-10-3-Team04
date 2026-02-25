package com.back.domain.item.item.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

data class ItemCreateRequest(
    @JvmField val categoryId: Long,

    @field:NotBlank
    @JvmField val name: String,

    @JvmField val imgUrl: String?,
    @JvmField val image: MultipartFile?,
    val startDate: LocalDate?,

    @field:NotBlank
    @JvmField val cycleDays: String
) {
    fun resolvedStartDate(): LocalDate {
        return startDate ?: LocalDate.now()
    }
}