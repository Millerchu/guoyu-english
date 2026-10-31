package com.guoyu.english.auth.domain;

import java.time.Instant;

/**
 * 会话过期和 PIN 延迟使用的时钟，测试可以替换。
 */
public interface TimeSource {

    Instant now();
}
