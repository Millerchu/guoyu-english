package com.guoyu.english.auth;

/**
 * 断言失败时不把秘密写进测试输出。
 */
public final class SecretText {

    private SecretText() {
    }

    public static void assertAbsent(String haystack, String secret) {
        if (haystack != null && secret != null && !secret.isEmpty() && haystack.contains(secret)) {
            throw new AssertionError("output leaked a secret");
        }
    }
}
