package com.guoyu.english.media;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.guoyu.english.support.RepoPaths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 图片哈希以最终 SVG 字节为准，清单和迁移里的值必须一致。
 */
class AnimalImageManifestTest {

    private static final List<String> NAMES = List.of("cat", "dog", "rabbit", "bird", "fish");

    @Test
    void manifestAndMigrationMatchTheSvgBytes() throws Exception {
        Path imageDir = RepoPaths.mediaSamples().resolve("image/animals");
        String manifest = Files.readString(imageDir.resolve("MANIFEST.md"), StandardCharsets.UTF_8);
        String migration = Files.readString(
                RepoPaths.repoRoot().resolve("server/src/main/resources/db/migration/V2__course_media.sql"),
                StandardCharsets.UTF_8);
        try (Stream<Path> files = Files.list(imageDir)) {
            assertThat(files.map(path -> path.getFileName().toString()))
                    .containsExactlyInAnyOrder(
                            "cat.svg", "dog.svg", "rabbit.svg", "bird.svg", "fish.svg", "MANIFEST.md");
        }
        for (String name : NAMES) {
            String storageKey = "image/animals/" + name + ".svg";
            byte[] bytes = Files.readAllBytes(imageDir.resolve(name + ".svg"));
            String svg = new String(bytes, StandardCharsets.UTF_8);
            String hash = sha256(bytes);
            assertThat(svg).doesNotContain("<script");
            assertThat(svg).doesNotContain("<text");
            assertThat(svg).doesNotContain("xlink:href");
            assertThat(svg.toLowerCase()).doesNotContain(name);
            assertThat(manifest).contains("| path | " + storageKey + " |");
            assertThat(manifest).contains("| sha256 | " + hash + " |");
            assertThat(manifest).contains("| size_bytes | " + bytes.length + " |");
            assertThat(manifest).contains("| source | HOMEMADE |");
            assertThat(manifest).contains("| mime | image/svg+xml |");
            assertThat(migration).contains("'" + storageKey + "'");
            assertThat(migration).contains("'" + hash + "'");
            assertThat(migration).contains(Integer.toString(bytes.length));
        }
        assertThat(migration).doesNotContain("child_profile");
        assertThat(migration).doesNotContain("client_device");
        assertThat(migration.toLowerCase()).doesNotContain("password");
        try (Stream<Path> migrations = Files.list(
                RepoPaths.repoRoot().resolve("server/src/main/resources/db/migration"))) {
            assertThat(migrations.map(path -> path.getFileName().toString()))
                    .containsExactlyInAnyOrder(
                            "V1__baseline.sql",
                            "V2__course_media.sql",
                            "V3__family_child_device.sql");
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte current : digest) {
            hex.append(String.format("%02x", current));
        }
        return hex.toString();
    }
}
