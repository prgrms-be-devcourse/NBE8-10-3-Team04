package com.back.global.globalExceptionHandler

import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.rsData.RsData
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    // @Slf4j 어노테이션은 코틀린에서 직접 지원하지 않으므로, LoggerFactory를 이용해 프로퍼티로 선언
    private val log = LoggerFactory.getLogger(this::class.java)

    // Void? 와 같은 불필요한 Nullable 제네릭을 제거하고 와일드카드(*)를 사용
    // 사용하지 않는 매개변수(HttpServletResponse?)를 제거
    @ExceptionHandler(ServiceException::class)
    fun handleServiceException(ex: ServiceException): ResponseEntity<RsData<*>> {
        val rsData = ex.rsData

        // if (ex.errorCode != null) 대신 safe call(?.)과 let 스코프 함수를 사용
        ex.errorCode?.let {
            log.warn("ServiceException 발생: code={}, message={}", it, rsData.msg)
        }

        val httpStatus = HttpStatus.valueOf(ex.statusCode)
        return ResponseEntity(rsData, httpStatus)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElementException(ex: NoSuchElementException): ResponseEntity<RsData<*>> {
        log.warn("NoSuchElementException 발생: {}", ex.message)
        return ResponseEntity(
            RsData<Any>(ErrorCode.DATA_NOT_FOUND.code, ErrorCode.DATA_NOT_FOUND.message),
            ErrorCode.DATA_NOT_FOUND.httpStatus
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<RsData<*>> {
        log.warn("ConstraintViolationException 발생: {}", ex.message)

        // 복잡했던 Java Stream API를 Kotlin Collection 확장 함수(map, sorted, joinToString)로 변경했습니다.
        // 문자열 포맷팅(String.formatted) 대신 직관적인 문자열 템플릿("${}")을 사용
        val message = ex.constraintViolations
            .map { violation ->
                // 정규식 split 대신 확장 함수 substringAfter를 사용
                val field = violation.propertyPath.toString().substringAfter('.')
                val messageTemplateBits = violation.messageTemplate.split(".").filter { it.isNotEmpty() }
                val code = messageTemplateBits.getOrNull(messageTemplateBits.size - 2) ?: ""

                // getter(getMessage()) 대신 프로퍼티(message) 접근 방식을 사용
                "$field-$code-${violation.message}"
            }
            .sorted()
            .joinToString("\n")

        return ResponseEntity(
            RsData<Any>(ErrorCode.INVALID_INPUT_VALUE.code, message),
            ErrorCode.INVALID_INPUT_VALUE.httpStatus
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ResponseEntity<RsData<*>> {
        log.warn("MethodArgumentNotValidException 발생")

        // filter와 map 캐스팅 단계를 filterIsInstance<FieldError>() 단일 호출
        val message = ex.bindingResult.allErrors
            .filterIsInstance<FieldError>()
            .map { "${it.field}-${it.code}-${it.defaultMessage}" }
            .sorted()
            .joinToString("\n")

        return ResponseEntity(
            RsData<Any>(ErrorCode.INVALID_INPUT_VALUE.code, message),
            ErrorCode.INVALID_INPUT_VALUE.httpStatus
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ResponseEntity<RsData<*>> {
        log.warn("HttpMessageNotReadableException 발생: {}", ex.message)
        return ResponseEntity(
            RsData<Any>(ErrorCode.INVALID_REQUEST_BODY.code, ErrorCode.INVALID_REQUEST_BODY.message),
            ErrorCode.INVALID_REQUEST_BODY.httpStatus
        )
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingRequestHeaderException(ex: MissingRequestHeaderException): ResponseEntity<RsData<*>> {
        log.warn("MissingRequestHeaderException 발생: {}", ex.headerName)

        // getter가 아닌 프로퍼티 접근으로 변경하고 문자열 템플릿을 적용
        val message = "${ex.headerName}-NotBlank-${ex.localizedMessage}"

        return ResponseEntity(
            RsData<Any>(ErrorCode.INVALID_INPUT_VALUE.code, message),
            ErrorCode.INVALID_INPUT_VALUE.httpStatus
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<RsData<*>> {
        log.error("Unexpected exception 발생", ex)
        return ResponseEntity(
            RsData<Any>(ErrorCode.INTERNAL_SERVER_ERROR.code, ErrorCode.INTERNAL_SERVER_ERROR.message),
            ErrorCode.INTERNAL_SERVER_ERROR.httpStatus
        )
    }
}