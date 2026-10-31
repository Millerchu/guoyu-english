package com.guoyu.english.common.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 持久化层使用带偏移的时间，业务层使用 Instant。数据库列是 timestamptz。
 */
public final class Timestamps {

    private Timestamps() {
    }

    public static OffsetDateTime offset(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    public static Instant instant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
