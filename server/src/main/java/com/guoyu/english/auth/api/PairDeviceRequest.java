package com.guoyu.english.auth.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * 配对正文只有配对码、设备编号和平台。pin 字段出现就拒绝，避免变成第二条换令牌入口。
 */
public class PairDeviceRequest {

    private String pairCode;

    private String deviceId;

    private String platform;

    private String pin;

    private String newPin;

    private final List<String> unknownFields = new ArrayList<>();

    @JsonAnySetter
    public void captureUnknown(String name, Object ignored) {
        unknownFields.add(name);
    }

    public boolean hasUnknownFields() {
        return !unknownFields.isEmpty();
    }

    public boolean containsPin() {
        return pin != null || newPin != null;
    }

    public String getPairCode() {
        return pairCode;
    }

    public void setPairCode(String pairCode) {
        this.pairCode = pairCode;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getNewPin() {
        return newPin;
    }

    public void setNewPin(String newPin) {
        this.newPin = newPin;
    }

    @Override
    public String toString() {
        return "PairDeviceRequest{redacted}";
    }
}
