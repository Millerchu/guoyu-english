package com.guoyu.english.auth.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 家长会话。这不是设备令牌。
 */
@JsonPropertyOrder({"parentToken", "expiresAt"})
public class ParentSessionResponse {

    private String parentToken;

    private String expiresAt;

    public String getParentToken() {
        return parentToken;
    }

    public void setParentToken(String parentToken) {
        this.parentToken = parentToken;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public String toString() {
        return "ParentSessionResponse{redacted}";
    }
}
