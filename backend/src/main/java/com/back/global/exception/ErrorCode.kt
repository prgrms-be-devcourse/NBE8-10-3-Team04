package com.back.global.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val code: String,
    val message: String,
    val httpStatus: HttpStatus
) {
    // 400 Bad Request
    INVALID_INPUT_VALUE("400-1", "잘못된 입력값입니다.", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_BODY("400-1", "요청 본문이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    SAME_PASSWORD("400-1", "새 비밀번호는 현재 비밀번호와 달라야 합니다.", HttpStatus.BAD_REQUEST),
    INACTIVE_ITEM_CANNOT_REPLACE("400-2", "비활성 상태의 아이템은 교체할 수 없습니다.", HttpStatus.BAD_REQUEST),

    // 401 Unauthorized
    LOGIN_REQUIRED("401-1", "로그인이 필요합니다.", HttpStatus.UNAUTHORIZED),
    INVALID_LOGIN_ID("401-1", "존재하지 않는 아이디입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_PASSWORD("401-1", "비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_AUTH_HEADER("401-2", "Authorization 헤더가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN_CLAIM("401-3", "토큰 클레임이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("401-4", "토큰이 만료되었습니다. 다시 로그인해주세요.", HttpStatus.UNAUTHORIZED),
    INVALID_API_KEY("401-5", "API 키가 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),

    // 403 Forbidden
    PASSWORD_MISMATCH("403-1", "현재 비밀번호가 일치하지 않습니다.", HttpStatus.FORBIDDEN),
    NO_PERMISSION("403-1", "권한이 없습니다.", HttpStatus.FORBIDDEN),

    // 404 Not Found
    ITEM_NOT_FOUND("404-1", "존재하지 않는 아이템입니다.", HttpStatus.NOT_FOUND),
    ITEM_NOT_FOUND_OR_NO_PERMISSION("404-1", "존재하지 않는 아이템이거나 권한이 없습니다.", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND("404-1", "존재하지 않는 유저입니다.", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND("404-1", "존재하지 않는 카테고리입니다.", HttpStatus.NOT_FOUND),
    DATA_NOT_FOUND("404-1", "해당 데이터가 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    ONGOING_HISTORY_NOT_FOUND("404-1", "진행중인 이력이 없습니다.", HttpStatus.NOT_FOUND),
    AI_ITEM_NOT_FOUND("404", "권장 주기를 찾을 수 없는 소모품입니다.", HttpStatus.NOT_FOUND),

    // 409 Conflict
    DUPLICATE_LOGIN_ID("409-1", "이미 존재하는 아이디입니다.", HttpStatus.CONFLICT),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR("500", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    IMAGE_UPLOAD_FAILED("500-1", "이미지 업로드 실패", HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_SEND_FAILED("500-1", "메일 발송 실패", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_TIMEOUT("500", "AI 응답 시간 초과", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_ERROR("500", "AI 처리 중 오류 발생", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_NO_RESPONSE("500", "AI로부터 응답을 받지 못했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_INVALID_JSON("500", "AI 응답이 유효한 JSON 형식이 아닙니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    JSON_PARSING_ERROR("500", "JSON 파싱 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    // 메시지에 동적 인자가 필요한 경우 사용 (String.format 기능)
    fun getMessageWithArgs(vararg args: Any?): String {
        return message.format(*args)
    }
}