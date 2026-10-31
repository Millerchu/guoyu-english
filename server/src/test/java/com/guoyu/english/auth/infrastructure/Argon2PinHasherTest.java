package com.guoyu.english.auth.infrastructure;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

import com.guoyu.english.auth.SecretText;

import static org.assertj.core.api.Assertions.assertThat;

class Argon2PinHasherTest {

    @Test
    void usesArgon2idWithTheDesignParameters() {
        String pin = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        Argon2PinHasher hasher = new Argon2PinHasher(new AuthProperties());
        String hash = hasher.hash(pin);
        String other = hasher.hash(pin);

        assertThat(hash).startsWith("$argon2id$");
        assertThat(hash).contains("m=65536,t=3,p=1");
        SecretText.assertAbsent(hash, pin);
        assertThat(hasher.matches(pin, hash)).isTrue();
        assertThat(hasher.matches(pin + "0", hash)).isFalse();
        assertThat(other).isNotEqualTo(hash);
    }
}
