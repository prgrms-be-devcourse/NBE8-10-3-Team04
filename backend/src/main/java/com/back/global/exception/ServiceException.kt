package com.back.global.exception

import com.back.global.rsData.RsData

// 주 생성자를 제거하고, 모든 초기화를 부 생성자(constructor) 내부에서 명시적으로 처리
class ServiceException : RuntimeException {

    val errorCode: ErrorCode?
    private val explicitMessage: String?
    private val legacyCode: String?

    // ErrorCode만 사용하는 생성자
    constructor(errorCode: ErrorCode) : super(errorCode.message) {
        this.errorCode = errorCode
        this.explicitMessage = null
        this.legacyCode = null
    }

    // ErrorCode와 커스텀 메시지를 사용하는 생성자
    constructor(errorCode: ErrorCode, customMessage: String?) : super(customMessage ?: errorCode.message) {
        this.errorCode = errorCode
        this.explicitMessage = customMessage
        this.legacyCode = null
    }

    // 메시지 포맷팅(가변 인자)을 지원하는 생성자
    constructor(errorCode: ErrorCode, vararg args: Any?) : super(errorCode.getMessageWithArgs(*args)) {
        this.errorCode = errorCode
        this.explicitMessage = errorCode.getMessageWithArgs(*args)
        this.legacyCode = null
    }

    // 하위 호환성 (Legacy) - ErrorCode 없이 코드와 메시지만 있는 경우
    @Deprecated("ErrorCode를 사용하는 생성자를 권장합니다.")
    constructor(resultCode: String, msg: String) : super("$resultCode : $msg") {
        this.errorCode = null
        this.explicitMessage = msg
        this.legacyCode = resultCode
    }

    val rsData: RsData<Nothing>
        get() = RsData(
            resultCode = resultCode,
            msg = explicitMessage ?: errorCode?.message ?: "에러 발생"
        )

    val resultCode: String
        get() = errorCode?.code ?: legacyCode ?: "500"

    val statusCode: Int
        get() = errorCode?.httpStatus?.value()
            ?: resultCode.substringBefore("-").toIntOrNull()
            ?: 500
}