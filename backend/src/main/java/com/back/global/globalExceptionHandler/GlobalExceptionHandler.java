package com.back.global.globalExceptionHandler;

import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 공통 예외 처리
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<RsData<Void>> handleServiceException(
            ServiceException ex,
            HttpServletResponse response
    ) {
        RsData<Void> rsData = ex.getRsData();

        // 로깅 추가
        if (ex.getErrorCode() != null) {
            log.warn("ServiceException 발생: code={}, message={}",
                    ex.getErrorCode(),
                    rsData.getMsg()
            );
        }

        // 500으로 고정하지 않고, 문자열 앞 3자리(예: "400-1" -> 400)를 상태 코드로 변환
        HttpStatus httpStatus = HttpStatus.valueOf(ex.getStatusCode());

        return new ResponseEntity<>(rsData, httpStatus);
    }

    // 값이 존재하지 않을 때
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<RsData<Void>> handleNoSuchElementException(NoSuchElementException ex) {
        log.warn("NoSuchElementException 발생: {}", ex.getMessage());

        return new ResponseEntity<>(
                new RsData<>(
                        ErrorCode.DATA_NOT_FOUND.getCode(),
                        ErrorCode.DATA_NOT_FOUND.getMessage()
                ),
                ErrorCode.DATA_NOT_FOUND.getHttpStatus()
        );
    }

    // @Validated 검증 실패 처리
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RsData<Void>> handleConstraintViolationException(
            ConstraintViolationException ex
    ) {
        log.warn("ConstraintViolationException 발생: {}", ex.getMessage());

        String message = ex.getConstraintViolations()
                .stream()
                .map(violation -> {
                    String field = violation.getPropertyPath().toString().split("\\.", 2)[1];
                    String[] messageTemplateBits = violation.getMessageTemplate().split("\\.");
                    String code = messageTemplateBits[messageTemplateBits.length - 2];
                    String _message = violation.getMessage();

                    return "%s-%s-%s".formatted(field, code, _message);
                })
                .sorted(Comparator.comparing(String::toString))
                .collect(Collectors.joining("\n"));

        return new ResponseEntity<>(
                new RsData<>(
                        ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        message
                ),
                ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()
        );
    }

    // @Valid 검증 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex
    ) {
        log.warn("MethodArgumentNotValidException 발생");

        String message = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .filter(error -> error instanceof FieldError)
                .map(error -> (FieldError) error)
                .map(error -> error.getField() + "-" + error.getCode() + "-" + error.getDefaultMessage())
                .sorted(Comparator.comparing(String::toString))
                .collect(Collectors.joining("\n"));

        return new ResponseEntity<>(
                new RsData<>(
                        ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        message
                ),
                ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()
        );
    }

    // JSON 형식 오류 처리
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RsData<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex
    ) {
        log.warn("HttpMessageNotReadableException 발생: {}", ex.getMessage());

        return new ResponseEntity<>(
                new RsData<>(
                        ErrorCode.INVALID_REQUEST_BODY.getCode(),
                        ErrorCode.INVALID_REQUEST_BODY.getMessage()
                ),
                ErrorCode.INVALID_REQUEST_BODY.getHttpStatus()
        );
    }

    // 요청 헤더 누락 처리
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<RsData<Void>> handleMissingRequestHeaderException(
            MissingRequestHeaderException ex
    ) {
        log.warn("MissingRequestHeaderException 발생: {}", ex.getHeaderName());

        String message = "%s-%s-%s".formatted(
                ex.getHeaderName(),
                "NotBlank",
                ex.getLocalizedMessage()
        );

        return new ResponseEntity<>(
                new RsData<>(
                        ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        message
                ),
                ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()
        );
    }

    // 예상치 못한 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RsData<Void>> handleException(Exception ex) {
        log.error("Unexpected exception 발생", ex);

        return new ResponseEntity<>(
                new RsData<>(
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
                ),
                ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus()
        );
    }
}
