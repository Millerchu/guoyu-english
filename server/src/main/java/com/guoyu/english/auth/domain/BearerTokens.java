package com.guoyu.english.auth.domain;

/**
 * 从 Authorization 头取出 Bearer 令牌。不记录头的内容。
 */
public final class BearerTokens {

    private static final String PREFIX = "Bearer ";

    private BearerTokens() {
    }

    public static String token(String header) {
        if (header == null || header.length() <= PREFIX.length()) {
            return null;
        }
        if (!header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            return null;
        }
        String token = header.substring(PREFIX.length()).trim();
        if (token.isEmpty() || token.indexOf(' ') >= 0) {
            return null;
        }
        return token;
    }
}
