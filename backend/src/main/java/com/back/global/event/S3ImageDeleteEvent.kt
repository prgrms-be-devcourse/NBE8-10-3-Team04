package com.back.global.event

data class S3ImageDeleteEvent(
    val imageUrls: List<String>
) {
    // 단건 URL을 위한 부생성자
    constructor(imageUrl: String) : this(listOf(imageUrl))
}