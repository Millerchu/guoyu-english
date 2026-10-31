package com.guoyu.english.auth.infrastructure;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.guoyu.english.auth.domain.TimeSource;

/**
 * 生产环境使用 UTC 系统时钟。
 */
@Component
public class SystemTimeSource implements TimeSource {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
