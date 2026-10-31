package com.guoyu.english.auth.domain;

import java.time.Duration;

/**
 * 连续失败 5 次之后才开始延迟。次数含本次失败：5、6、7、8 次及以上对应四档。
 */
public final class PinDelayPolicy {

    public static final Duration AFTER_FIVE = Duration.ofSeconds(30);

    public static final Duration AFTER_SIX = Duration.ofMinutes(1);

    public static final Duration AFTER_SEVEN = Duration.ofMinutes(5);

    public static final Duration AFTER_EIGHT = Duration.ofMinutes(15);

    private PinDelayPolicy() {
    }

    public static Duration delayAfterFailure(int failedCount) {
        if (failedCount < 5) {
            return Duration.ZERO;
        }
        if (failedCount == 5) {
            return AFTER_FIVE;
        }
        if (failedCount == 6) {
            return AFTER_SIX;
        }
        if (failedCount == 7) {
            return AFTER_SEVEN;
        }
        return AFTER_EIGHT;
    }
}
