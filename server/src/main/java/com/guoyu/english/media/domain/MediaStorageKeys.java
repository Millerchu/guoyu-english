package com.guoyu.english.media.domain;

import java.util.regex.Pattern;

import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;

/**
 * 媒体键只允许相对路径。绝对路径、盘符和目录穿越都不能出现在响应里。
 */
public final class MediaStorageKeys {

    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    private MediaStorageKeys() {
    }

    public static String requireRelative(String storageKey) {
        if (!isRelative(storageKey)) {
            throw broken();
        }
        return storageKey;
    }

    public static String requireSha256(String sha256) {
        if (sha256 == null) {
            throw broken();
        }
        String normalized = sha256.trim();
        if (!SHA256.matcher(normalized).matches()) {
            throw broken();
        }
        return normalized;
    }

    static boolean isRelative(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return false;
        }
        if (storageKey.startsWith("/") || storageKey.startsWith("\\") || storageKey.contains("..")
                || storageKey.contains("\\") || storageKey.contains(":") || storageKey.contains("//")) {
            return false;
        }
        for (int i = 0; i < storageKey.length(); i++) {
            char current = storageKey.charAt(i);
            if (current <= ' ' || Character.isISOControl(current)) {
                return false;
            }
        }
        return true;
    }

    private static ApiException broken() {
        return new ApiException(ApiCode.INTERNAL_ERROR, ApiCode.INTERNAL_ERROR.name());
    }
}
