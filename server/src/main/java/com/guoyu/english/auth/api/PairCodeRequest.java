package com.guoyu.english.auth.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * 家长会话签发配对码时指定设备名和儿童范围。
 */
public class PairCodeRequest {

    private String deviceName;

    private List<Long> childIds;

    private final List<String> unknownFields = new ArrayList<>();

    @JsonAnySetter
    public void captureUnknown(String name, Object ignored) {
        unknownFields.add(name);
    }

    public boolean hasUnknownFields() {
        return !unknownFields.isEmpty();
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public List<Long> getChildIds() {
        return childIds;
    }

    public void setChildIds(List<Long> childIds) {
        this.childIds = childIds;
    }

    @Override
    public String toString() {
        return "PairCodeRequest{childCount=" + (childIds == null ? 0 : childIds.size()) + "}";
    }
}
