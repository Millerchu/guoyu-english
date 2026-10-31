package com.guoyu.english.child.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * 建档请求。昵称不进入 toString。
 */
public class CreateChildRequest {

    private String nickname;

    private String ageBand;

    private Integer dailyMinutes;

    private String timezoneId;

    private final List<String> unknownFields = new ArrayList<>();

    @JsonAnySetter
    public void captureUnknown(String name, Object ignored) {
        unknownFields.add(name);
    }

    public boolean hasUnknownFields() {
        return !unknownFields.isEmpty();
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

    @Override
    public String toString() {
        return "CreateChildRequest{redacted}";
    }
}
