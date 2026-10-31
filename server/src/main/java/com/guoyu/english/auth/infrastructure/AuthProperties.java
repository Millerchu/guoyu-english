package com.guoyu.english.auth.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 身份参数。引导码默认空，空值表示不能建家庭。Argon2 默认值对应详设 26.5。
 */
@ConfigurationProperties(prefix = "guoyu.auth")
public class AuthProperties {

    private String bootstrapCode = "";

    private Duration parentSessionTtl = Duration.ofMinutes(15);

    private Duration pairCodeTtl = Duration.ofMinutes(5);

    private Duration bootstrapRetryWindow = Duration.ofMinutes(10);

    private int argon2MemoryKib = 65536;

    private int argon2Iterations = 3;

    private int argon2Parallelism = 1;

    private int argon2SaltLength = 16;

    private int argon2HashLength = 32;

    public String getBootstrapCode() {
        return bootstrapCode;
    }

    public void setBootstrapCode(String bootstrapCode) {
        this.bootstrapCode = bootstrapCode;
    }

    public Duration getParentSessionTtl() {
        return parentSessionTtl;
    }

    public void setParentSessionTtl(Duration parentSessionTtl) {
        this.parentSessionTtl = parentSessionTtl;
    }

    public Duration getPairCodeTtl() {
        return pairCodeTtl;
    }

    public void setPairCodeTtl(Duration pairCodeTtl) {
        this.pairCodeTtl = pairCodeTtl;
    }

    public Duration getBootstrapRetryWindow() {
        return bootstrapRetryWindow;
    }

    public void setBootstrapRetryWindow(Duration bootstrapRetryWindow) {
        this.bootstrapRetryWindow = bootstrapRetryWindow;
    }

    public int getArgon2MemoryKib() {
        return argon2MemoryKib;
    }

    public void setArgon2MemoryKib(int argon2MemoryKib) {
        this.argon2MemoryKib = argon2MemoryKib;
    }

    public int getArgon2Iterations() {
        return argon2Iterations;
    }

    public void setArgon2Iterations(int argon2Iterations) {
        this.argon2Iterations = argon2Iterations;
    }

    public int getArgon2Parallelism() {
        return argon2Parallelism;
    }

    public void setArgon2Parallelism(int argon2Parallelism) {
        this.argon2Parallelism = argon2Parallelism;
    }

    public int getArgon2SaltLength() {
        return argon2SaltLength;
    }

    public void setArgon2SaltLength(int argon2SaltLength) {
        this.argon2SaltLength = argon2SaltLength;
    }

    public int getArgon2HashLength() {
        return argon2HashLength;
    }

    public void setArgon2HashLength(int argon2HashLength) {
        this.argon2HashLength = argon2HashLength;
    }
}
