package com.guoyu.english.auth.domain;

import java.util.UUID;

/**
 * 已通过校验的家长会话。deviceId 为空表示引导会话，还没有绑定设备。
 */
public class ParentPrincipal {

    private final long familyId;

    private final long sessionId;

    private final UUID deviceId;

    public ParentPrincipal(long familyId, long sessionId, UUID deviceId) {
        this.familyId = familyId;
        this.sessionId = sessionId;
        this.deviceId = deviceId;
    }

    public long getFamilyId() {
        return familyId;
    }

    public long getSessionId() {
        return sessionId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }
}
