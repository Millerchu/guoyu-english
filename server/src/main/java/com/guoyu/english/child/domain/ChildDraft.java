package com.guoyu.english.child.domain;

/**
 * 校验后的建档内容。昵称已经去掉首尾空白。
 */
public class ChildDraft {

    private final String nickname;

    private final AgeBand ageBand;

    private final int dailyMinutes;

    private final String timezoneId;

    public ChildDraft(String nickname, AgeBand ageBand, int dailyMinutes, String timezoneId) {
        this.nickname = nickname;
        this.ageBand = ageBand;
        this.dailyMinutes = dailyMinutes;
        this.timezoneId = timezoneId;
    }

    public String getNickname() {
        return nickname;
    }

    public AgeBand getAgeBand() {
        return ageBand;
    }

    public int getDailyMinutes() {
        return dailyMinutes;
    }

    public String getTimezoneId() {
        return timezoneId;
    }
}
