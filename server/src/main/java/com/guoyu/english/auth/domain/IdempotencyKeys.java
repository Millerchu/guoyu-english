package com.guoyu.english.auth.domain;

import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;

/**
 * 幂等键由调用方传入。限制长度和控制字符，避免写进日志时断行。
 */
public final class IdempotencyKeys {

    public static final int MAX_LENGTH = 128;

    private IdempotencyKeys() {
    }

    public static void check(String key) {
        if (key == null || key.isBlank() || key.length() > MAX_LENGTH) {
            throw invalid();
        }
        for (int i = 0; i < key.length(); i++) {
            char current = key.charAt(i);
            if (current <= ' ' || current == 127 || Character.isISOControl(current)) {
                throw invalid();
            }
        }
    }

    private static ApiException invalid() {
        return new ApiException(ApiCode.INVALID_ARGUMENT, ApiCode.INVALID_ARGUMENT.name());
    }
}
