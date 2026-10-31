package com.guoyu.english.auth.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 引导成功后的家庭编号和短期家长会话。明文令牌只放在这次响应里。
 */
@JsonPropertyOrder({"familyId", "parentToken", "expiresAt"})
public class BootstrapResponse {

    private Long familyId;

    private String parentToken;

    private String expiresAt;

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

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
        return "BootstrapResponse{familyId=" + familyId + ", redacted}";
    }
}
