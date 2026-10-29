package com.guoyu.english.common.web;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdsTest {

    @Test
    void keepsCallerRequestId() {
        String requestId = "11111111-2222-4333-8444-555555555555";
        assertThat(RequestIds.resolve(requestId)).isEqualTo(requestId);
    }

    @Test
    void generatesUuidWhenHeaderMissingOrBlank() {
        assertThat(UUID.fromString(RequestIds.resolve(null))).isNotNull();
        assertThat(UUID.fromString(RequestIds.resolve("   "))).isNotNull();
        assertThat(UUID.fromString(RequestIds.resolve("\r\n"))).isNotNull();
    }

    @Test
    void stripsControlCharactersAndTruncates() {
        assertThat(RequestIds.resolve("bad\r\ninjected")).isEqualTo("badinjected");
        assertThat(RequestIds.resolve("a".repeat(200))).hasSize(128);
    }
}
