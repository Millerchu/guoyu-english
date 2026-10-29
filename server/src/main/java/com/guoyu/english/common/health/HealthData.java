package com.guoyu.english.common.health;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 健康检查的 data。只有运行状态和构建版本，不包含口令、地址或令牌。
 */
@JsonPropertyOrder({"status", "version"})
public class HealthData {

    public static final String STATUS_UP = "UP";

    private String status;

    private String version;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
