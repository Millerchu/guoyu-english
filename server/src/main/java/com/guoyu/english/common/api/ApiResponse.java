package com.guoyu.english.common.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * {@code /api/v1} 的统一 JSON 外壳。五个字段始终序列化，包括 data 为 null 的情况。
 *
 * @param <T> data 的类型
 */
@JsonPropertyOrder({"requestId", "code", "message", "data", "serverTime"})
public class ApiResponse<T> {

    private String requestId;

    private String code;

    private String message;

    private T data;

    private String serverTime;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getServerTime() {
        return serverTime;
    }

    public void setServerTime(String serverTime) {
        this.serverTime = serverTime;
    }
}
