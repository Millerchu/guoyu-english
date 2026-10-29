package com.guoyu.english.common.api;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import com.guoyu.english.common.web.RequestIds;

/**
 * 组装统一响应。serverTime 使用 UTC 的 ISO-8601，并以 Z 结尾。
 */
public final class ApiResponses {

    private static final DateTimeFormatter SERVER_TIME = DateTimeFormatter.ISO_INSTANT;

    private ApiResponses() {
    }

    public static <T> ApiResponse<T> ok(T data) {
        return of(ApiCode.OK, ApiCode.OK.name(), data);
    }

    public static <T> ApiResponse<T> of(ApiCode code, String message, T data) {
        ApiResponse<T> body = new ApiResponse<>();
        body.setRequestId(RequestIds.current());
        body.setCode(code.name());
        body.setMessage(message);
        body.setData(data);
        body.setServerTime(SERVER_TIME.format(Instant.now()));
        return body;
    }
}
