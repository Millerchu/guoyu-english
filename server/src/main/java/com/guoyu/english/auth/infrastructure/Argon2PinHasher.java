package com.guoyu.english.auth.infrastructure;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 家长 PIN 和引导请求指纹使用 Argon2id。参数来自配置，默认 64 MiB、3 次迭代、并行度 1。
 */
@Component
public class Argon2PinHasher {

    private final Argon2PasswordEncoder encoder;

    public Argon2PinHasher(AuthProperties properties) {
        this.encoder = new Argon2PasswordEncoder(
                properties.getArgon2SaltLength(),
                properties.getArgon2HashLength(),
                properties.getArgon2Parallelism(),
                properties.getArgon2MemoryKib(),
                properties.getArgon2Iterations());
    }

    public String hash(String secret) {
        return encoder.encode(secret);
    }

    public boolean matches(String secret, String hash) {
        return encoder.matches(secret, hash);
    }
}
