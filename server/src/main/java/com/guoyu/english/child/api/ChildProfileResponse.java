package com.guoyu.english.child.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 儿童档案的对外形状。issuedThrough 在 S14 之前保持 null。
 */
@JsonPropertyOrder({
        "childId",
        "nickname",
        "ageBand",
        "dailyMinutes",
        "timezoneId",
        "timezoneVersion",
        "profileVersion",
        "pendingTimezone",
        "pendingTimezoneEffectiveAt",
        "issuedThrough"
})
public class ChildProfileResponse {

    private Long childId;

    private String nickname;

    private String ageBand;

    private Integer dailyMinutes;

    private String timezoneId;

    private Integer timezoneVersion;

    private Integer profileVersion;

    private String pendingTimezone;

    private String pendingTimezoneEffectiveAt;

    private String issuedThrough;

    public Long getChildId() {
        return childId;
    }

    public void setChildId(Long childId) {
        this.childId = childId;
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

    public String getPendingTimezoneEffectiveAt() {
        return pendingTimezoneEffectiveAt;
    }

    public void setPendingTimezoneEffectiveAt(String pendingTimezoneEffectiveAt) {
        this.pendingTimezoneEffectiveAt = pendingTimezoneEffectiveAt;
    }

    public String getIssuedThrough() {
        return issuedThrough;
    }

    public void setIssuedThrough(String issuedThrough) {
        this.issuedThrough = issuedThrough;
    }
}
