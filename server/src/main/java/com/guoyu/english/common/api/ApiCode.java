package com.guoyu.english.common.api;

import org.springframework.http.HttpStatus;

/**
 * 统一响应里的业务码。名称与开发计划共享契约一致，后续阶段只引用，不改名。
 */
public enum ApiCode {

    OK(HttpStatus.OK),
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST),
    /**
     * 详设第 24.4 节写成 401/403。脚手架固定使用 401，表示设备令牌不再有效。
     */
    DEVICE_REVOKED(HttpStatus.UNAUTHORIZED),
    EVENT_CONFLICT(HttpStatus.CONFLICT),
    EVENT_ID_REUSED(HttpStatus.CONFLICT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ApiCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
