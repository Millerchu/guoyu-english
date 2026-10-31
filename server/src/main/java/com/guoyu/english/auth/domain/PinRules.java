package com.guoyu.english.auth.domain;

import java.util.regex.Pattern;

import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;

/**
 * 家长 PIN 只做 4–6 位数字格式检查。它是防误触，不是强密码。
 */
public final class PinRules {

    private static final Pattern PIN = Pattern.compile("^[0-9]{4,6}$");

    private PinRules() {
    }

    public static void check(String pin) {
        if (pin == null || !PIN.matcher(pin).matches()) {
            throw new ApiException(ApiCode.INVALID_ARGUMENT, ApiCode.INVALID_ARGUMENT.name());
        }
    }
}
