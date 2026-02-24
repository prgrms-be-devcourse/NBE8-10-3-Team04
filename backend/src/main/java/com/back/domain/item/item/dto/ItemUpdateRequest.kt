package com.back.domain.item.item.dto

import org.springframework.web.multipart.MultipartFile

data class ItemUpdateRequest(
    @JvmField val categoryId: Long?,
    @JvmField val name: String?,
    @JvmField val imgUrl: String?,
    @JvmField val image: MultipartFile?,
    @JvmField val cycleDays: String?,
    @JvmField val isActive: Boolean?
)
