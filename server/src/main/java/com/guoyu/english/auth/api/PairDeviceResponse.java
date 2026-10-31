package com.guoyu.english.auth.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 配对成功后的设备令牌。长期有效，直到撤销。
 */
@JsonPropertyOrder({"deviceId", "deviceToken", "childIds"})
public class PairDeviceResponse {

    private String deviceId;

    private String deviceToken;

    private List<Long> childIds = new ArrayList<>();

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public List<Long> getChildIds() {
        return childIds;
    }

    public void setChildIds(List<Long> childIds) {
        this.childIds = childIds;
    }

    @Override
    public String toString() {
        return "PairDeviceResponse{deviceId=" + deviceId + ", redacted}";
    }
}
