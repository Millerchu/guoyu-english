package com.guoyu.english.media.domain;

import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;

/**
 * 媒体来源。名称与开发计划共享契约一致，不另起一套。
 */
public enum MediaSource {

    HOMEMADE,
    LICENSED_THIRD_PARTY,
    AI_GENERATED,
    OFFLINE_TTS,
    PLACEHOLDER;

    /**
     * 把库里的来源码转成枚举。未知值说明种子或迁移坏了，不能当成合法来源返回。
     */
    public static MediaSource fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            throw broken(null);
        }
        try {
            return MediaSource.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw broken(exception);
        }
    }

    private static ApiException broken(Throwable cause) {
        if (cause == null) {
            return new ApiException(ApiCode.INTERNAL_ERROR, ApiCode.INTERNAL_ERROR.name());
        }
        return new ApiException(ApiCode.INTERNAL_ERROR, ApiCode.INTERNAL_ERROR.name(), cause);
    }
}
