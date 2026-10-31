package com.guoyu.english.auth.domain;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PinDelayPolicyTest {

    @Test
    void delayStartsAtFiveFailuresAndThenStepsUp() {
        assertThat(PinDelayPolicy.delayAfterFailure(1)).isEqualTo(Duration.ZERO);
        assertThat(PinDelayPolicy.delayAfterFailure(4)).isEqualTo(Duration.ZERO);
        assertThat(PinDelayPolicy.delayAfterFailure(5)).isEqualTo(Duration.ofSeconds(30));
        assertThat(PinDelayPolicy.delayAfterFailure(6)).isEqualTo(Duration.ofMinutes(1));
        assertThat(PinDelayPolicy.delayAfterFailure(7)).isEqualTo(Duration.ofMinutes(5));
        assertThat(PinDelayPolicy.delayAfterFailure(8)).isEqualTo(Duration.ofMinutes(15));
        assertThat(PinDelayPolicy.delayAfterFailure(12)).isEqualTo(Duration.ofMinutes(15));
    }
}
