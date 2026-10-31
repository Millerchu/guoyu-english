package com.guoyu.english.auth.infrastructure.persistence;

import java.time.OffsetDateTime;

/**
 * family_account 一行。一个库只允许一行。
 */
public class FamilyAccountRow {

    private Long id;

    private Integer authGeneration;

    private OffsetDateTime bootstrapConsumedAt;

    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getAuthGeneration() {
        return authGeneration;
    }

    public void setAuthGeneration(Integer authGeneration) {
        this.authGeneration = authGeneration;
    }

    public OffsetDateTime getBootstrapConsumedAt() {
        return bootstrapConsumedAt;
    }

    public void setBootstrapConsumedAt(OffsetDateTime bootstrapConsumedAt) {
        this.bootstrapConsumedAt = bootstrapConsumedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
