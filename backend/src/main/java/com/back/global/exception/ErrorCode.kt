package com.back.global.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val code: String,
    val message: String,
    val httpStatus: HttpStatus
) {
    // RsData의 파싱 구조를 유지하면서도 에러 식별성을 높이기 위해
    // code 포맷을 단순 숫자에서 '{HTTP상태코드}-{도메인접두사+일련번호}' 형태로 리팩토링
    // 도메인 분류: C(Common/System), U(User/Auth), I(Item/Category), AI(Artificial Intelligence)

    // --- 400 Bad Request ---
    // 공통(C) 요청 오류
    INVALID_INPUT_VALUE("400-C001", "잘못된 입력값입니다.", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_BODY("400-C002", "요청 본문이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_URL("400-C003", "잘못된 이미지 URL 형태입니다.", HttpStatus.BAD_REQUEST),

    // 유저(U) 관련 잘못된 요청
    SAME_PASSWORD("400-U001", "새 비밀번호는 현재 비밀번호와 달라야 합니다.", HttpStatus.BAD_REQUEST),

    // 아이템(I) 관련 잘못된 요청
    INACTIVE_ITEM_CANNOT_REPLACE("400-I001", "비활성 상태의 아이템은 교체할 수 없습니다.", HttpStatus.BAD_REQUEST),
    CYCLE_PERIOD_START_DATE_REQUIRED("400-I002", "기준 시작일(startDate)은 필수입니다.", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_CYCLE_PERIOD_UNIT("400-I003", "지원하지 않는 교체 주기 단위입니다.", HttpStatus.BAD_REQUEST),
    CYCLE_DAYS_REQUIRED("400-I004", "교체 주기(cycleDays) 값은 필수입니다.", HttpStatus.BAD_REQUEST),
    INVALID_CYCLE_DAYS_FORMAT("400-I005", "교체 주기 형식이 올바르지 않습니다. (예: 30d, 2m, 1y)", HttpStatus.BAD_REQUEST),
    CYCLE_DAYS_MUST_BE_POSITIVE("400-I006", "교체 주기 값은 1 이상이어야 합니다.", HttpStatus.BAD_REQUEST),

    // --- 401 Unauthorized ---
    // 유저/인증(U) 오류
    LOGIN_REQUIRED("401-U001", "로그인이 필요합니다.", HttpStatus.UNAUTHORIZED),
    INVALID_LOGIN_ID("401-U002", "존재하지 않는 아이디입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_PASSWORD("401-U003", "비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_AUTH_HEADER("401-U004", "Authorization 헤더가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN_CLAIM("401-U005", "토큰 클레임이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("401-U006", "토큰이 만료되었습니다. 다시 로그인해주세요.", HttpStatus.UNAUTHORIZED),
    INVALID_API_KEY("401-U007", "API 키가 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),

    // --- 403 Forbidden ---
    // 유저/권한(U) 오류
    PASSWORD_MISMATCH("403-U001", "현재 비밀번호가 일치하지 않습니다.", HttpStatus.FORBIDDEN),
    NO_PERMISSION("403-U002", "권한이 없습니다.", HttpStatus.FORBIDDEN),

    // --- 404 Not Found ---
    // 공통(C) 조회 오류
    DATA_NOT_FOUND("404-C001", "해당 데이터가 존재하지 않습니다.", HttpStatus.NOT_FOUND),

    // 아이템/카테고리(I) 조회 오류
    ITEM_NOT_FOUND("404-I001", "존재하지 않는 아이템입니다.", HttpStatus.NOT_FOUND),
    ITEM_NOT_FOUND_OR_NO_PERMISSION("404-I002", "존재하지 않는 아이템이거나 권한이 없습니다.", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND("404-I003", "존재하지 않는 카테고리입니다.", HttpStatus.NOT_FOUND),
    ONGOING_HISTORY_NOT_FOUND("404-I004", "진행중인 이력이 없습니다.", HttpStatus.NOT_FOUND),

    // 유저(U) 조회 오류
    USER_NOT_FOUND("404-U001", "존재하지 않는 유저입니다.", HttpStatus.NOT_FOUND),

    // AI 도메인 조회 오류
    AI_ITEM_NOT_FOUND("404-AI001", "권장 주기를 찾을 수 없는 소모품입니다.", HttpStatus.NOT_FOUND),

    // --- 409 Conflict ---
    // 유저(U) 충돌 오류
    DUPLICATE_LOGIN_ID("409-U001", "이미 존재하는 아이디입니다.", HttpStatus.CONFLICT),

    // --- 500 Internal Server Error ---
    // 공통/시스템(C) 오류
    INTERNAL_SERVER_ERROR("500-C001", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    IMAGE_UPLOAD_FAILED("500-C002", "이미지 업로드 실패", HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_SEND_FAILED("500-C003", "메일 발송 실패", HttpStatus.INTERNAL_SERVER_ERROR),
    JSON_PARSING_ERROR("500-C004", "JSON 파싱 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    IMAGE_DELETE_FAILED("500-C005", "이미지 삭제 실패", HttpStatus.INTERNAL_SERVER_ERROR),
    ENTITY_NOT_PERSISTED("500-C006", "엔티티가 아직 영속화되지 않았습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // AI 인공지능 오류
    AI_TIMEOUT("500-AI001", "AI 응답 시간 초과", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_ERROR("500-AI002", "AI 처리 중 오류 발생", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_NO_RESPONSE("500-AI003", "AI로부터 응답을 받지 못했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_INVALID_JSON("500-AI004", "AI 응답이 유효한 JSON 형식이 아닙니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    // 메시지에 동적 인자가 필요한 경우 사용 (String.format 기능)
    fun getMessageWithArgs(vararg args: Any?): String {
        return message.format(*args)
    }
}