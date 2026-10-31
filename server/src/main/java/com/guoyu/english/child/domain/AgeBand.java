package com.guoyu.english.child.domain;

import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;

/**
 * 5–7 岁年龄段。接口只接受这三个枚举名。
 */
public enum AgeBand {

    AGE_5,
    AGE_6,
    AGE_7;

    public static AgeBand parse(String raw) {
        if (raw == null) {
            throw invalid();
        }
        for (AgeBand band : values()) {
            if (band.name().equals(raw)) {
                return band;
            }
        }
        throw invalid();
    }

    private static ApiException invalid() {
        return new ApiException(ApiCode.INVALID_ARGUMENT, ApiCode.INVALID_ARGUMENT.name());
    }
}
