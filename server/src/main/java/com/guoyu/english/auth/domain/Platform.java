package com.guoyu.english.auth.domain;

import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;

/**
 * 设备平台。只接受这两个值，大小写必须一致。
 */
public enum Platform {

    ANDROID,
    H5;

    public static Platform parse(String raw) {
        if (raw == null) {
            throw new ApiException(ApiCode.INVALID_ARGUMENT, ApiCode.INVALID_ARGUMENT.name());
        }
        for (Platform platform : values()) {
            if (platform.name().equals(raw)) {
                return platform;
            }
        }
        throw new ApiException(ApiCode.INVALID_ARGUMENT, ApiCode.INVALID_ARGUMENT.name());
    }
}
