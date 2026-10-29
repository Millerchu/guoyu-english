package com.guoyu.english.common.api;

/**
 * 可预期的业务失败。响应 message 使用调用方给出的稳定文案，不把口令或令牌放进异常信息。
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ApiCode code;

    private final String clientMessage;

    public ApiException(ApiCode code, String clientMessage) {
        super(code.name());
        this.code = code;
        this.clientMessage = clientMessage;
    }

    public ApiException(ApiCode code, String clientMessage, Throwable cause) {
        super(code.name(), cause);
        this.code = code;
        this.clientMessage = clientMessage;
    }

    public ApiCode getCode() {
        return code;
    }

    public String getClientMessage() {
        return clientMessage == null || clientMessage.isBlank() ? code.name() : clientMessage;
    }
}
