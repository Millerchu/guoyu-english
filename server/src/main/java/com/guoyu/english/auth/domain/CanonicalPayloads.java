package com.guoyu.english.auth.domain;

/**
 * 用长度前缀拼请求指纹，避免字段粘连后被当成同一次请求。
 */
public final class CanonicalPayloads {

    private CanonicalPayloads() {
    }

    public static String join(String... parts) {
        StringBuilder builder = new StringBuilder();
        if (parts != null) {
            for (String part : parts) {
                String value = part == null ? "" : part;
                builder.append(value.length()).append(':').append(value).append('\n');
            }
        }
        return builder.toString();
    }
}
