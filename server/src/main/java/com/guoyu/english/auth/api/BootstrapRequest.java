package com.guoyu.english.auth.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * 引导请求。toString 不包含引导码和 PIN，避免被日志框架直接打出来。
 */
public class BootstrapRequest {

    private String bootstrapCode;

    private String newPin;

    private final List<String> unknownFields = new ArrayList<>();

    @JsonAnySetter
    public void captureUnknown(String name, Object ignored) {
        unknownFields.add(name);
    }

    public boolean hasUnknownFields() {
        return !unknownFields.isEmpty();
    }

    public String getBootstrapCode() {
        return bootstrapCode;
    }

    public void setBootstrapCode(String bootstrapCode) {
        this.bootstrapCode = bootstrapCode;
    }

    public String getNewPin() {
        return newPin;
    }

    public void setNewPin(String newPin) {
        this.newPin = newPin;
    }

    @Override
    public String toString() {
        return "BootstrapRequest{redacted}";
    }
}
