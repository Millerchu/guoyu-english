package com.guoyu.english.auth.application;

import com.guoyu.english.auth.api.ParentSessionResponse;
import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;

/**
 * PIN 校验的提交结果。失败也要先提交计数，再由调用方抛出业务异常。
 */
public class ParentSessionOutcome {

    private final ParentSessionResponse response;

    private final ApiException failure;

    private ParentSessionOutcome(ParentSessionResponse response, ApiException failure) {
        this.response = response;
        this.failure = failure;
    }

    public static ParentSessionOutcome success(ParentSessionResponse response) {
        return new ParentSessionOutcome(response, null);
    }

    public static ParentSessionOutcome failure(ApiCode code) {
        return new ParentSessionOutcome(null, new ApiException(code, code.name()));
    }

    public ParentSessionResponse required() {
        if (failure != null) {
            throw failure;
        }
        return response;
    }
}
