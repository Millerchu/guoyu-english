package com.guoyu.english.media;

import org.junit.jupiter.api.Test;

import com.guoyu.english.common.api.ApiException;
import com.guoyu.english.media.domain.MediaSource;
import com.guoyu.english.media.domain.MediaStorageKeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaStorageKeysTest {

    @Test
    void relativeAnimalPathsStayRelative() {
        assertThat(MediaStorageKeys.requireRelative("image/animals/cat.svg"))
                .isEqualTo("image/animals/cat.svg");
        assertThat(MediaStorageKeys.requireRelative("audio/animals/cat-v1.mp3"))
                .isEqualTo("audio/animals/cat-v1.mp3");
    }

    @Test
    void absoluteAndTraversalKeysAreRejected() {
        assertThatThrownBy(() -> MediaStorageKeys.requireRelative("/data/media/cat.svg"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> MediaStorageKeys.requireRelative("../audio/cat-v1.mp3"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> MediaStorageKeys.requireRelative("C:/media/cat.svg"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> MediaStorageKeys.requireRelative("image//animals/cat.svg"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void sha256MustBeLowerHex() {
        String hash = "25162a46867b2ccecfd3c4acca4fc947a4b65716ec84d1e919a5c999e0c7560d";
        assertThat(MediaStorageKeys.requireSha256("  " + hash + "  ")).isEqualTo(hash);
        assertThatThrownBy(() -> MediaStorageKeys.requireSha256("ZZ")).isInstanceOf(ApiException.class);
    }

    @Test
    void sourceNamesMatchTheContract() {
        assertThat(MediaSource.fromDatabase("HOMEMADE")).isEqualTo(MediaSource.HOMEMADE);
        assertThat(MediaSource.fromDatabase("OFFLINE_TTS")).isEqualTo(MediaSource.OFFLINE_TTS);
        assertThatThrownBy(() -> MediaSource.fromDatabase("DOWNLOAD")).isInstanceOf(ApiException.class);
    }
}
