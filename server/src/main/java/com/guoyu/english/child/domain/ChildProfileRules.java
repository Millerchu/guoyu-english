package com.guoyu.english.child.domain;

import java.time.DateTimeException;
import java.time.ZoneId;

import com.guoyu.english.auth.domain.CanonicalPayloads;
import com.guoyu.english.child.api.CreateChildRequest;
import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;

/**
 * 建档字段规则。时区默认 Asia/Shanghai，每日分钟数默认 15。
 */
public final class ChildProfileRules {

    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    public static final int DEFAULT_DAILY_MINUTES = 15;

    public static final int MIN_DAILY_MINUTES = 10;

    public static final int MAX_DAILY_MINUTES = 30;

    public static final int MAX_NICKNAME_CODE_POINTS = 20;

    private ChildProfileRules() {
    }

    public static ChildDraft normalize(CreateChildRequest request) {
        if (request == null || request.hasUnknownFields()) {
            throw invalid();
        }
        String nickname = normalizeNickname(request.getNickname());
        AgeBand ageBand = AgeBand.parse(request.getAgeBand());
        int dailyMinutes = normalizeMinutes(request.getDailyMinutes());
        String timezoneId = normalizeTimezone(request.getTimezoneId());
        return new ChildDraft(nickname, ageBand, dailyMinutes, timezoneId);
    }

    public static String fingerprint(ChildDraft draft) {
        return CanonicalPayloads.join(
                draft.getNickname(),
                draft.getAgeBand().name(),
                Integer.toString(draft.getDailyMinutes()),
                draft.getTimezoneId());
    }

    private static String normalizeNickname(String raw) {
        if (raw == null) {
            throw invalid();
        }
        String nickname = raw.trim();
        int length = nickname.codePointCount(0, nickname.length());
        if (length < 1 || length > MAX_NICKNAME_CODE_POINTS) {
            throw invalid();
        }
        for (int i = 0; i < nickname.length(); i++) {
            if (Character.isISOControl(nickname.charAt(i))) {
                throw invalid();
            }
        }
        return nickname;
    }

    private static int normalizeMinutes(Integer dailyMinutes) {
        int minutes = dailyMinutes == null ? DEFAULT_DAILY_MINUTES : dailyMinutes;
        if (minutes < MIN_DAILY_MINUTES || minutes > MAX_DAILY_MINUTES) {
            throw invalid();
        }
        return minutes;
    }

    private static String normalizeTimezone(String raw) {
        String timezoneId = raw == null || raw.isBlank() ? DEFAULT_TIMEZONE : raw.trim();
        if (!timezoneId.contains("/")) {
            throw invalid();
        }
        try {
            ZoneId zoneId = ZoneId.of(timezoneId);
            if (!zoneId.getId().equals(timezoneId)) {
                throw invalid();
            }
        } catch (DateTimeException exception) {
            throw invalid();
        }
        return timezoneId;
    }

    private static ApiException invalid() {
        return new ApiException(ApiCode.INVALID_ARGUMENT, ApiCode.INVALID_ARGUMENT.name());
    }
}
