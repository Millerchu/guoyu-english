package com.guoyu.english.auth.domain;

import java.util.UUID;

/**
 * 当前仍有效的设备。撤销后不能再变成这个对象。
 */
public class DevicePrincipal {

    private final long familyId;

    private final UUID deviceId;

    public DevicePrincipal(long familyId, UUID deviceId) {
        this.familyId = familyId;
        this.deviceId = deviceId;
    }

    public long getFamilyId() {
        return familyId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }
}
