package com.guoyu.english.auth.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 撤销结果。重复撤销仍然返回已撤销。
 */
@JsonPropertyOrder({"deviceId", "revoked"})
public class RevokeDeviceResponse {

    private String deviceId;

    private boolean revoked;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }
}
