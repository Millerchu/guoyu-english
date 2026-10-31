package com.guoyu.english.child.infrastructure.persistence;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * child_profile 一行。issued_through 由后续阶段写入。
 */
public class ChildProfileRow {

    private Long id;

    private Long familyId;

    private String nickname;

    private String ageBand;

    private Integer dailyMinutes;

    private String timezoneId;

    private Integer timezoneVersion;

    private Integer profileVersion;

    private String pendingTimezone;

    private OffsetDateTime pendingTimezoneEffectiveAt;

    private LocalDate issuedThrough;

    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAgeBand() {
        return ageBand;
    }

    public void setAgeBand(String ageBand) {
        this.ageBand = ageBand;
    }

    public Integer getDailyMinutes() {
        return dailyMinutes;
    }

    public void setDailyMinutes(Integer dailyMinutes) {
        this.dailyMinutes = dailyMinutes;
    }

    public String getTimezoneId() {
        return timezoneId;
    }

    public void setTimezoneId(String timezoneId) {
        this.timezoneId = timezoneId;
    }

    public Integer getTimezoneVersion() {
        return timezoneVersion;
    }

    public void setTimezoneVersion(Integer timezoneVersion) {
        this.timezoneVersion = timezoneVersion;
    }

    public Integer getProfileVersion() {
        return profileVersion;
    }

    public void setProfileVersion(Integer profileVersion) {
        this.profileVersion = profileVersion;
    }

    public String getPendingTimezone() {
        return pendingTimezone;
    }

    public void setPendingTimezone(String pendingTimezone) {
        this.pendingTimezone = pendingTimezone;
    }

    public OffsetDateTime getPendingTimezoneEffectiveAt() {
        return pendingTimezoneEffectiveAt;
    }

    public void setPendingTimezoneEffectiveAt(OffsetDateTime pendingTimezoneEffectiveAt) {
        this.pendingTimezoneEffectiveAt = pendingTimezoneEffectiveAt;
    }

    public LocalDate getIssuedThrough() {
        return issuedThrough;
    }

    public void setIssuedThrough(LocalDate issuedThrough) {
        this.issuedThrough = issuedThrough;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
