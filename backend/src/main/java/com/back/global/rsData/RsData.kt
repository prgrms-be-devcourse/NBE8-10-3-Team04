package com.back.global.rsData

import com.fasterxml.jackson.annotation.JsonIgnore

data class RsData<T> @JvmOverloads constructor(
    val resultCode: String,
    val msg: String,
    val data: T? = null
) {
    // statusCode는 resultCode에서 자동으로 계산되는 프로퍼티 (JSON 응답에서는 제외)
    @get:JsonIgnore
    val statusCode: Int
        get() = resultCode.substringBefore("-").toInt()
}