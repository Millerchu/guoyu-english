package com.guoyu.english.auth.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 某个家庭在某台设备上的 PIN 失败次数和锁定截止时间。
 */
public class PinAttemptRow {

    private Long familyId;

    private UUID deviceId;

    private Integer failedCount;

    private OffsetDateTime lockedUntil;

    private OffsetDateTime updatedAt;

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(OffsetDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
