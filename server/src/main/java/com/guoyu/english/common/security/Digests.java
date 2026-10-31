package com.guoyu.english.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 令牌和配对码的单向摘要。家长 PIN 不走这里，PIN 使用 Argon2id。
 */
public final class Digests {

    private Digests() {
    }

    public static String sha256Hex(String value) {
        return toHex(sha256(value == null ? "" : value));
    }

    /**
     * 先摘要再比较，避免明文长度差异走快速返回。
     */
    public static boolean constantEquals(String expected, String actual) {
        byte[] left = sha256(expected == null ? "" : expected);
        byte[] right = sha256(actual == null ? "" : actual);
        return MessageDigest.isEqual(left, right);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte current : bytes) {
            builder.append(Character.forDigit((current >> 4) & 0xF, 16));
            builder.append(Character.forDigit(current & 0xF, 16));
        }
        return builder.toString();
    }
}
