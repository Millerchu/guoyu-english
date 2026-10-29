package com.guoyu.english.common.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;
import com.guoyu.english.common.api.ApiResponse;
import com.guoyu.english.common.api.ApiResponses;

/**
 * 把已识别的失败收成统一响应。响应文案只用业务码，不回传异常详情、口令或令牌。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(ApiException exception) {
        if (exception.getCause() == null) {
            log.warn("业务异常, requestId={}, code={}", RequestIds.current(), exception.getCode().name());
        } else {
            log.warn("业务异常, requestId={}, code={}", RequestIds.current(), exception.getCode().name(), exception);
        }
        return respond(exception.getCode(), exception.getClientMessage());
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleInvalidArgument(Exception exception) {
        log.warn("请求参数无效, requestId={}, type={}", RequestIds.current(), exception.getClass().getName());
        return respond(ApiCode.INVALID_ARGUMENT, ApiCode.INVALID_ARGUMENT.name());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("未处理异常, requestId={}", RequestIds.current(), exception);
        return respond(ApiCode.INTERNAL_ERROR, ApiCode.INTERNAL_ERROR.name());
    }

    private ResponseEntity<ApiResponse<Void>> respond(ApiCode code, String message) {
        return ResponseEntity.status(code.httpStatus()).body(ApiResponses.of(code, message, null));
    }
}
