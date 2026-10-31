package com.guoyu.english.auth.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * 用已绑定设备换家长会话。设备令牌放在 Authorization 头，不放在正文里。
 */
public class ParentSessionRequest {

    private String pin;

    private final List<String> unknownFields = new ArrayList<>();

    @JsonAnySetter
    public void captureUnknown(String name, Object ignored) {
        unknownFields.add(name);
    }

    public boolean hasUnknownFields() {
        return !unknownFields.isEmpty();
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    @Override
    public String toString() {
        return "ParentSessionRequest{redacted}";
    }
}
