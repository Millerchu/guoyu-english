package com.guoyu.english.common.web;

import java.util.UUID;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 读取或生成请求标识。优先使用请求头 {@code X-Request-Id}，否则生成 UUID。
 */
public final class RequestIds {

    public static final String HEADER = "X-Request-Id";

    public static final String ATTRIBUTE = "com.guoyu.english.requestId";

    private static final int MAX_LENGTH = 128;

    private RequestIds() {
    }

    /**
     * 保留调用方传入的标识，并去掉回车、换行和其他控制字符，避免日志注入。
     */
    public static String resolve(String header) {
        if (header == null) {
            return UUID.randomUUID().toString();
        }
        StringBuilder cleaned = new StringBuilder(Math.min(header.length(), MAX_LENGTH));
        for (int i = 0; i < header.length() && cleaned.length() < MAX_LENGTH; i++) {
            char current = header.charAt(i);
            if (current == '\r' || current == '\n' || Character.isISOControl(current)) {
                continue;
            }
            cleaned.append(current);
        }
        String value = cleaned.toString().trim();
        if (value.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return value;
    }

    public static String current() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            Object value = servletAttributes.getRequest().getAttribute(ATTRIBUTE);
            if (value instanceof String requestId && !requestId.isBlank()) {
                return requestId;
            }
        }
        return UUID.randomUUID().toString();
    }
}
