package com.guoyu.english.auth.infrastructure.persistence;

import java.time.OffsetDateTime;

/**
 * 家长 PIN 的 Argon2id 哈希。不保存明文。
 */
public class ParentCredentialRow {

    private Long familyId;

    private String pinHash;

    private OffsetDateTime updatedAt;

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
