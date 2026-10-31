package com.guoyu.english.auth.domain;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PinRulesTest {

    @Test
    void acceptsFourToSixDigits() {
        int value = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        PinRules.check(String.format("%04d", value % 10_000));
        PinRules.check(String.format("%05d", value % 100_000));
        PinRules.check(String.format("%06d", value));
    }

    @Test
    void rejectsOtherShapes() {
        int value = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        assertRejected(null);
        assertRejected("");
        assertRejected(String.format("%03d", value % 1_000));
        assertRejected(String.format("%07d", 1_000_000 + value));
        assertRejected("abcd");
        assertRejected(String.format("%04d", value % 10_000) + " ");
    }

    private void assertRejected(String pin) {
        assertThatThrownBy(() -> PinRules.check(pin))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ApiCode.INVALID_ARGUMENT);
    }
}
