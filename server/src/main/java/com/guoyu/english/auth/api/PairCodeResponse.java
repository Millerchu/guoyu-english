package com.guoyu.english.auth.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 5 分钟一次性配对码。明文只出现在这次响应里。
 */
@JsonPropertyOrder({"pairCode", "expiresAt", "childIds"})
public class PairCodeResponse {

    private String pairCode;

    private String expiresAt;

    private List<Long> childIds = new ArrayList<>();

    public String getPairCode() {
        return pairCode;
    }

    public void setPairCode(String pairCode) {
        this.pairCode = pairCode;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public List<Long> getChildIds() {
        return childIds;
    }

    public void setChildIds(List<Long> childIds) {
        this.childIds = childIds;
    }

    @Override
    public String toString() {
        return "PairCodeResponse{redacted}";
    }
}
