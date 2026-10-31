package com.guoyu.english.auth.infrastructure;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

/**
 * 签发家长会话、设备令牌和一次性配对码。明文只出现在当次响应里。
 */
@Component
public class SecretGenerator {

    public static final String PARENT_PREFIX = "pt.";

    public static final String DEVICE_PREFIX = "dt.";

    public static final String PAIR_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public static final int PAIR_CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String parentToken() {
        return PARENT_PREFIX + randomToken();
    }

    public String deviceToken() {
        return DEVICE_PREFIX + randomToken();
    }

    public String pairCode() {
        StringBuilder code = new StringBuilder(PAIR_CODE_LENGTH);
        for (int i = 0; i < PAIR_CODE_LENGTH; i++) {
            code.append(PAIR_ALPHABET.charAt(random.nextInt(PAIR_ALPHABET.length())));
        }
        return code.toString();
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
