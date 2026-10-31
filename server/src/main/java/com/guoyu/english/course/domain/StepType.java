package com.guoyu.english.course.domain;

import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;

/**
 * 课程步骤类型。GAME 留给以后的小游戏，animals-001 版本 1 的种子不插入这一步。
 */
public enum StepType {

    LISTEN,
    SPEAK,
    RECOGNIZE,
    READ,
    GAME;

    public static StepType fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            throw broken(null);
        }
        try {
            return StepType.valueOf(value);
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
