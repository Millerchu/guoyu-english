package com.guoyu.english.support;

import java.nio.file.Path;

/**
 * Maven 在 server/ 下跑测试。素材在仓库根的 media-samples/。
 */
public final class RepoPaths {

    private RepoPaths() {
    }

    public static Path repoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        if ("server".equals(String.valueOf(current.getFileName()))) {
            return current.getParent();
        }
        return current;
    }

    public static Path mediaSamples() {
        return repoRoot().resolve("media-samples");
    }
}
