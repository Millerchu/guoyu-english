-- S04: course, lexeme, media and recognize questions.
-- Business string ids follow V1.9 section 23.2. Child, device and reward tables stay out of V2.
-- 句子正文在 lesson_sentence。definition_json 只存有序 id，避免说和读各写一份。
-- 音频行引用 metadata.json 的路径、哈希和 TTS 字段，不重写 mp3。

CREATE TABLE course_theme (
    theme_id varchar(64) PRIMARY KEY,
    name_zh varchar(64) NOT NULL,
    name_en varchar(64) NOT NULL,
    CONSTRAINT course_theme_id_shape CHECK (theme_id ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

CREATE TABLE media_asset (
    asset_id varchar(80) PRIMARY KEY,
    storage_key varchar(255) NOT NULL,
    source varchar(32) NOT NULL,
    tool varchar(255),
    voice varchar(64),
    style varchar(64),
    speech_rate numeric(3, 2),
    pause_between_words_ms integer,
    pause_between_sentences_ms integer,
    pause_after_prompt_ms integer,
    source_text text,
    generated_at timestamptz,
    mime varchar(64) NOT NULL,
    size_bytes bigint NOT NULL,
    sha256 char(64) NOT NULL,
    CONSTRAINT media_asset_storage_key_unique UNIQUE (storage_key),
    CONSTRAINT media_asset_source_known CHECK (
        source IN ('HOMEMADE', 'LICENSED_THIRD_PARTY', 'AI_GENERATED', 'OFFLINE_TTS', 'PLACEHOLDER')
    ),
    CONSTRAINT media_asset_storage_key_relative CHECK (
        storage_key ~ '^[a-z0-9][a-z0-9./_-]*$'
        AND storage_key !~ '\.\.'
        AND position('//' in storage_key) = 0
    ),
    CONSTRAINT media_asset_sha256_hex CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT media_asset_size_positive CHECK (size_bytes > 0),
    CONSTRAINT media_asset_mime_present CHECK (mime <> ''),
    CONSTRAINT media_asset_speech_rate_range CHECK (
        speech_rate IS NULL OR (speech_rate >= 0.75 AND speech_rate <= 0.85)
    ),
    CONSTRAINT media_asset_pauses_non_negative CHECK (
        (pause_between_words_ms IS NULL OR pause_between_words_ms >= 0)
        AND (pause_between_sentences_ms IS NULL OR pause_between_sentences_ms >= 0)
        AND (pause_after_prompt_ms IS NULL OR pause_after_prompt_ms >= 0)
    ),
    CONSTRAINT media_asset_offline_tts_complete CHECK (
        source <> 'OFFLINE_TTS'
        OR (
            tool IS NOT NULL
            AND voice IS NOT NULL
            AND style IS NOT NULL
            AND speech_rate IS NOT NULL
            AND pause_between_words_ms IS NOT NULL
            AND pause_between_sentences_ms IS NOT NULL
            AND pause_after_prompt_ms IS NOT NULL
            AND source_text IS NOT NULL
            AND generated_at IS NOT NULL
        )
    )
);

CREATE TABLE lexeme (
    lexeme_id varchar(64) PRIMARY KEY,
    theme_id varchar(64) NOT NULL REFERENCES course_theme (theme_id),
    english varchar(64) NOT NULL,
    chinese varchar(64) NOT NULL,
    CONSTRAINT lexeme_id_shape CHECK (lexeme_id ~ '^lexeme-[a-z0-9]+(-[a-z0-9]+)*$')
);

CREATE TABLE lesson (
    lesson_id varchar(64) NOT NULL,
    version integer NOT NULL,
    theme_id varchar(64) NOT NULL REFERENCES course_theme (theme_id),
    PRIMARY KEY (lesson_id, version),
    CONSTRAINT lesson_id_shape CHECK (lesson_id ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT lesson_version_positive CHECK (version > 0)
);

CREATE TABLE lesson_step (
    lesson_id varchar(64) NOT NULL,
    lesson_version integer NOT NULL,
    step_id varchar(64) NOT NULL,
    step_type varchar(32) NOT NULL,
    sort_order integer NOT NULL,
    required_for_first_task boolean NOT NULL,
    definition_json jsonb NOT NULL,
    PRIMARY KEY (lesson_id, lesson_version, step_id),
    UNIQUE (lesson_id, lesson_version, sort_order),
    CONSTRAINT lesson_step_type_known CHECK (
        step_type IN ('LISTEN', 'SPEAK', 'RECOGNIZE', 'READ', 'GAME')
    ),
    CONSTRAINT lesson_step_order_positive CHECK (sort_order > 0),
    CONSTRAINT lesson_step_definition_object CHECK (jsonb_typeof(definition_json) = 'object'),
    FOREIGN KEY (lesson_id, lesson_version) REFERENCES lesson (lesson_id, version)
);

CREATE TABLE lesson_lexeme (
    lesson_id varchar(64) NOT NULL,
    lesson_version integer NOT NULL,
    lexeme_id varchar(64) NOT NULL REFERENCES lexeme (lexeme_id),
    image_asset_id varchar(80) NOT NULL REFERENCES media_asset (asset_id),
    audio_asset_id varchar(80) NOT NULL REFERENCES media_asset (asset_id),
    sort_order integer NOT NULL,
    PRIMARY KEY (lesson_id, lesson_version, lexeme_id),
    UNIQUE (lesson_id, lesson_version, sort_order),
    CONSTRAINT lesson_lexeme_order_positive CHECK (sort_order > 0),
    FOREIGN KEY (lesson_id, lesson_version) REFERENCES lesson (lesson_id, version)
);

CREATE TABLE lesson_sentence (
    lesson_id varchar(64) NOT NULL,
    lesson_version integer NOT NULL,
    sentence_id varchar(64) NOT NULL,
    english varchar(256) NOT NULL,
    chinese varchar(256) NOT NULL,
    audio_asset_id varchar(80) NOT NULL REFERENCES media_asset (asset_id),
    sort_order integer NOT NULL,
    PRIMARY KEY (lesson_id, lesson_version, sentence_id),
    UNIQUE (lesson_id, lesson_version, sort_order),
    CONSTRAINT lesson_sentence_id_shape CHECK (sentence_id ~ '^sent-[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT lesson_sentence_order_positive CHECK (sort_order > 0),
    FOREIGN KEY (lesson_id, lesson_version) REFERENCES lesson (lesson_id, version)
);

CREATE TABLE lesson_step_sentence (
    lesson_id varchar(64) NOT NULL,
    lesson_version integer NOT NULL,
    step_id varchar(64) NOT NULL,
    sentence_id varchar(64) NOT NULL,
    sort_order integer NOT NULL,
    PRIMARY KEY (lesson_id, lesson_version, step_id, sort_order),
    UNIQUE (lesson_id, lesson_version, step_id, sentence_id),
    CONSTRAINT lesson_step_sentence_order_positive CHECK (sort_order > 0),
    FOREIGN KEY (lesson_id, lesson_version, step_id)
        REFERENCES lesson_step (lesson_id, lesson_version, step_id),
    FOREIGN KEY (lesson_id, lesson_version, sentence_id)
        REFERENCES lesson_sentence (lesson_id, lesson_version, sentence_id)
);

CREATE TABLE lesson_question (
    lesson_id varchar(64) NOT NULL,
    lesson_version integer NOT NULL,
    question_id varchar(64) NOT NULL,
    step_id varchar(64) NOT NULL,
    correct_lexeme_id varchar(64) NOT NULL REFERENCES lexeme (lexeme_id),
    sort_order integer NOT NULL,
    PRIMARY KEY (lesson_id, lesson_version, question_id),
    UNIQUE (lesson_id, lesson_version, sort_order),
    CONSTRAINT lesson_question_id_shape CHECK (question_id ~ '^q-[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT lesson_question_order_positive CHECK (sort_order > 0),
    FOREIGN KEY (lesson_id, lesson_version) REFERENCES lesson (lesson_id, version),
    FOREIGN KEY (lesson_id, lesson_version, step_id)
        REFERENCES lesson_step (lesson_id, lesson_version, step_id)
);

CREATE TABLE lesson_option (
    lesson_id varchar(64) NOT NULL,
    lesson_version integer NOT NULL,
    option_id varchar(64) NOT NULL,
    lexeme_id varchar(64) NOT NULL REFERENCES lexeme (lexeme_id),
    PRIMARY KEY (lesson_id, lesson_version, option_id),
    UNIQUE (lesson_id, lesson_version, lexeme_id),
    CONSTRAINT lesson_option_id_shape CHECK (option_id ~ '^opt-[a-z0-9]+(-[a-z0-9]+)*$'),
    FOREIGN KEY (lesson_id, lesson_version) REFERENCES lesson (lesson_id, version)
);

CREATE TABLE lesson_question_option (
    lesson_id varchar(64) NOT NULL,
    lesson_version integer NOT NULL,
    question_id varchar(64) NOT NULL,
    option_id varchar(64) NOT NULL,
    sort_order integer NOT NULL,
    PRIMARY KEY (lesson_id, lesson_version, question_id, sort_order),
    UNIQUE (lesson_id, lesson_version, question_id, option_id),
    CONSTRAINT lesson_question_option_order_positive CHECK (sort_order > 0),
    FOREIGN KEY (lesson_id, lesson_version, question_id)
        REFERENCES lesson_question (lesson_id, lesson_version, question_id),
    FOREIGN KEY (lesson_id, lesson_version, option_id)
        REFERENCES lesson_option (lesson_id, lesson_version, option_id)
);

CREATE FUNCTION guoyu_question_options_valid()
RETURNS trigger
LANGUAGE plpgsql
AS $guoyu$
DECLARE
    target_lesson_id varchar(64);
    target_version integer;
    target_question_id varchar(64);
    option_count integer;
    correct_count integer;
BEGIN
    IF TG_OP = 'DELETE' THEN
        target_lesson_id := OLD.lesson_id;
        target_version := OLD.lesson_version;
        target_question_id := OLD.question_id;
    ELSE
        target_lesson_id := NEW.lesson_id;
        target_version := NEW.lesson_version;
        target_question_id := NEW.question_id;
    END IF;

    SELECT count(*)
      INTO option_count
      FROM lesson_question_option
     WHERE lesson_id = target_lesson_id
       AND lesson_version = target_version
       AND question_id = target_question_id;

    IF option_count < 2 OR option_count > 3 THEN
        RAISE EXCEPTION 'question option count must be 2 or 3';
    END IF;

    SELECT count(*)
      INTO correct_count
      FROM lesson_question AS q
      JOIN lesson_question_option AS qo
        ON qo.lesson_id = q.lesson_id
       AND qo.lesson_version = q.lesson_version
       AND qo.question_id = q.question_id
      JOIN lesson_option AS lo
        ON lo.lesson_id = qo.lesson_id
       AND lo.lesson_version = qo.lesson_version
       AND lo.option_id = qo.option_id
     WHERE q.lesson_id = target_lesson_id
       AND q.lesson_version = target_version
       AND q.question_id = target_question_id
       AND lo.lexeme_id = q.correct_lexeme_id;

    IF correct_count <> 1 THEN
        RAISE EXCEPTION 'correct lexeme must be one of the options';
    END IF;

    RETURN NULL;
END;
$guoyu$;

CREATE CONSTRAINT TRIGGER lesson_question_option_count
    AFTER INSERT OR UPDATE OR DELETE ON lesson_question_option
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION guoyu_question_options_valid();


INSERT INTO course_theme (theme_id, name_zh, name_en)
VALUES ('theme-animals', $txt$动物$txt$, 'Animals');

INSERT INTO media_asset (
    asset_id, storage_key, source, tool, voice, style, speech_rate,
    pause_between_words_ms, pause_between_sentences_ms, pause_after_prompt_ms,
    source_text, generated_at, mime, size_bytes, sha256
) VALUES (
    'image-animals-cat',
    'image/animals/cat.svg',
    'HOMEMADE',
    'handmade-svg',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    timestamptz '2026-10-31T00:00:00Z',
    'image/svg+xml',
    1328,
    '25162a46867b2ccecfd3c4acca4fc947a4b65716ec84d1e919a5c999e0c7560d'
);

INSERT INTO media_asset (
    asset_id, storage_key, source, tool, voice, style, speech_rate,
    pause_between_words_ms, pause_between_sentences_ms, pause_after_prompt_ms,
    source_text, generated_at, mime, size_bytes, sha256
) VALUES (
    'image-animals-dog',
    'image/animals/dog.svg',
    'HOMEMADE',
    'handmade-svg',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    timestamptz '2026-10-31T00:00:00Z',
    'image/svg+xml',
    1070,
    'bb11e2492c0e3334ed74707a14bfbc52220c9420554559c0195ed8b2a8ee5505'
);

INSERT INTO media_asset (
    asset_id, storage_key, source, tool, voice, style, speech_rate,
    pause_between_words_ms, pause_between_sentences_ms, pause_after_prompt_ms,
    source_text, generated_at, mime, size_bytes, sha256
) VALUES (
    'image-animals-rabbit',
    'image/animals/rabbit.svg',
    'HOMEMADE',
    'handmade-svg',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    timestamptz '2026-10-31T00:00:00Z',
    'image/svg+xml',
    1156,
    '5b09d471384252854949428a6a500db5f77f3c353f0ab8dccfa56ec7ae16a362'
);

INSERT INTO media_asset (
    asset_id, storage_key, source, tool, voice, style, speech_rate,
    pause_between_words_ms, pause_between_sentences_ms, pause_after_prompt_ms,
    source_text, generated_at, mime, size_bytes, sha256
) VALUES (
    'image-animals-bird',
    'image/animals/bird.svg',
    'HOMEMADE',
    'handmade-svg',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    timestamptz '2026-10-31T00:00:00Z',
    'image/svg+xml',
    1034,
    '5860b1847001eeea187c549ed75ee1b8d6539084d34e201ae4c48bb862cede89'
);

INSERT INTO media_asset (
    asset_id, storage_key, source, tool, voice, style, speech_rate,
    pause_between_words_ms, pause_between_sentences_ms, pause_after_prompt_ms,
    source_text, generated_at, mime, size_bytes, sha256
) VALUES (
    'image-animals-fish',
    'image/animals/fish.svg',
    'HOMEMADE',
    'handmade-svg',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    timestamptz '2026-10-31T00:00:00Z',
    'image/svg+xml',
    1000,
    'f18a295336d726280af04b4b77be49b2b48f602d9f72dbcf3b69caac5bb0dce4'
);

INSERT INTO media_asset (
    asset_id, storage_key, source, tool, voice, style, speech_rate,
    pause_between_words_ms, pause_between_sentences_ms, pause_after_prompt_ms,
    source_text, generated_at, mime, size_bytes, sha256
) VALUES (
    'audio-animals-cat',
    'audio/animals/cat-v1.mp3',
    'OFFLINE_TTS',
    $txt$espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5$txt$,
    'en-us+m3',
    'clear-warm',
    0.80,
    600,
    900,
    400,
    $txt$cat$txt$,
    timestamptz '2026-10-29T15:05:20Z',
    'audio/mpeg',
    21850,
    '695032ccdd3f18889eb362db351a62cdcb705bf020b55f7d240ae90aba324ad5'
);

INSERT INTO media_asset (
    asset_id, storage_key, source, tool, voice, style, speech_rate,
    pause_between_words_ms, pause_between_sentences_ms, pause_after_prompt_ms,
    source_text, generated_at, mime, size_bytes, sha256
) VALUES (
    'audio-animals-dog',
    'audio/animals/dog-v1.mp3',
    'OFFLINE_TTS',
    $txt$espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5$txt$,
    'en-us+m3',
    'clear-warm',
    0.80,
    600,
    900,
    400,
    $txt$dog$txt$,
    timestamptz '2026-10-29T15:05:20Z',
    'audio/mpeg',
    22163,
    '7bbf0624102a19a549d235d2bd9a53b6d53afbb183fa948aba26e179b1c2c7fb'
);

INSERT INTO media_asset (
    asset_id, storage_key, source, tool, voice, style, speech_rate,
    pause_between_words_ms, pause_between_sentences_ms, pause_after_prompt_ms,
    source_text, generated_at, mime, size_bytes, sha256
) VALUES (
    'audio-animals-rabbit',
    'audio/animals/rabbit-v1.mp3',
    'OFFLINE_TTS',
    $txt$espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5$txt$,
    'en-us+m3',
    'clear-warm',
    0.80,
    600,
    900,
    400,
    $txt$rabbit$txt$,
    timestamptz '2026-10-29T15:05:20Z',
    'audio/mpeg',
    23734,
    'd4c7fa819868b98c9f7957cf3831579be6cbde890750a84d003410e660556ba3'
);

INSERT INTO media_asset (
    asset_id, storage_key, source, tool, voice, style, speech_rate,
    pause_between_words_ms, pause_between_sentences_ms, pause_after_prompt_ms,
    source_text, generated_at, mime, size_bytes, sha256
) VALUES (
    'audio-animals-bird',
    'audio/animals/bird-v1.mp3',
    'OFFLINE_TTS',
    $txt$espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5$txt$,
    'en-us+m3',
    'clear-warm',
    0.80,
    600,
    900,
    400,
    $txt$bird$txt$,
    timestamptz '2026-10-29T15:05:20Z',
    'audio/mpeg',
    22164,
    'e55a9f25313d69167e0ed58da9d8d4aaa44e21f0603778647d329198ad94bd81'
);

INSERT INTO media_asset (
    asset_id, storage_key, source, tool, voice, style, speech_rate,
    pause_between_words_ms, pause_between_sentences_ms, pause_after_prompt_ms,
    source_text, generated_at, mime, size_bytes, sha256
) VALUES (
    'audio-animals-fish',
    'audio/animals/fish-v1.mp3',
    'OFFLINE_TTS',
    $txt$espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5$txt$,
    'en-us+m3',
    'clear-warm',
    0.80,
    600,
    900,
    400,
    $txt$fish$txt$,
    timestamptz '2026-10-29T15:05:20Z',
    'audio/mpeg',
    22164,
    'ff5731d063f4106bc5986ab5a34d41d1dd68e21a614648b5eaf1782306553e1b'
);

INSERT INTO media_asset (
    asset_id, storage_key, source, tool, voice, style, speech_rate,
    pause_between_words_ms, pause_between_sentences_ms, pause_after_prompt_ms,
    source_text, generated_at, mime, size_bytes, sha256
) VALUES (
    'audio-animals-i-see-a-cat',
    'audio/animals/i-see-a-cat-v1.mp3',
    'OFFLINE_TTS',
    $txt$espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5$txt$,
    'en-us+m3',
    'clear-warm',
    0.80,
    600,
    900,
    400,
    $txt$I see a cat.$txt$,
    timestamptz '2026-10-29T15:05:20Z',
    'audio/mpeg',
    55400,
    '738e6154eafab886f8148af93f5c9963718c6d0b46094b70d88e9ca1f6fc9cb0'
);

INSERT INTO media_asset (
    asset_id, storage_key, source, tool, voice, style, speech_rate,
    pause_between_words_ms, pause_between_sentences_ms, pause_after_prompt_ms,
    source_text, generated_at, mime, size_bytes, sha256
) VALUES (
    'audio-animals-i-see-a-dog',
    'audio/animals/i-see-a-dog-v1.mp3',
    'OFFLINE_TTS',
    $txt$espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5$txt$,
    'en-us+m3',
    'clear-warm',
    0.80,
    600,
    900,
    400,
    $txt$I see a dog.$txt$,
    timestamptz '2026-10-29T15:05:20Z',
    'audio/mpeg',
    55714,
    '8e694823e36d4eacf138885a3255ea1754395e5b91d22a09da7af5b9dfb03766'
);

INSERT INTO media_asset (
    asset_id, storage_key, source, tool, voice, style, speech_rate,
    pause_between_words_ms, pause_between_sentences_ms, pause_after_prompt_ms,
    source_text, generated_at, mime, size_bytes, sha256
) VALUES (
    'audio-animals-i-like-rabbits',
    'audio/animals/i-like-rabbits-v1.mp3',
    'OFFLINE_TTS',
    $txt$espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5$txt$,
    'en-us+m3',
    'clear-warm',
    0.80,
    600,
    900,
    400,
    $txt$I like rabbits.$txt$,
    timestamptz '2026-10-29T15:05:20Z',
    'audio/mpeg',
    49134,
    'f955de2d731be18ea8236e9b72eac2b904d40d2b0c42b0c2fa21dd425c797a01'
);

INSERT INTO media_asset (
    asset_id, storage_key, source, tool, voice, style, speech_rate,
    pause_between_words_ms, pause_between_sentences_ms, pause_after_prompt_ms,
    source_text, generated_at, mime, size_bytes, sha256
) VALUES (
    'audio-animals-its-a-dog',
    'audio/animals/its-a-dog-v1.mp3',
    'OFFLINE_TTS',
    $txt$espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5$txt$,
    'en-us+m3',
    'clear-warm',
    0.80,
    600,
    900,
    400,
    $txt$It's a dog.$txt$,
    timestamptz '2026-10-29T15:05:20Z',
    'audio/mpeg',
    44428,
    '28bbd9fdcd346a23ac9884f8427e116f8cca1fd51ac09431cd53a193ad91ea8a'
);

INSERT INTO media_asset (
    asset_id, storage_key, source, tool, voice, style, speech_rate,
    pause_between_words_ms, pause_between_sentences_ms, pause_after_prompt_ms,
    source_text, generated_at, mime, size_bytes, sha256
) VALUES (
    'audio-animals-whats-this',
    'audio/animals/whats-this-v1.mp3',
    'OFFLINE_TTS',
    $txt$espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5$txt$,
    'en-us+m3',
    'clear-warm',
    0.80,
    600,
    900,
    400,
    $txt$What's this?$txt$,
    timestamptz '2026-10-29T15:05:20Z',
    'audio/mpeg',
    35338,
    '1cf49505d116239143ed0854625d56aaf9def7ca406e41045b807713c5229b41'
);

INSERT INTO lexeme (lexeme_id, theme_id, english, chinese) VALUES
    ('lexeme-cat', 'theme-animals', 'cat', $txt$猫$txt$),
    ('lexeme-dog', 'theme-animals', 'dog', $txt$狗$txt$),
    ('lexeme-rabbit', 'theme-animals', 'rabbit', $txt$兔子$txt$),
    ('lexeme-bird', 'theme-animals', 'bird', $txt$鸟$txt$),
    ('lexeme-fish', 'theme-animals', 'fish', $txt$鱼$txt$);

INSERT INTO lesson (lesson_id, version, theme_id)
VALUES ('animals-001', 1, 'theme-animals');

INSERT INTO lesson_step (
    lesson_id, lesson_version, step_id, step_type, sort_order, required_for_first_task, definition_json
) VALUES
    ('animals-001', 1, 'step-listen', 'LISTEN', 1, true, '{"lexemeIds": ["lexeme-cat", "lexeme-dog", "lexeme-rabbit", "lexeme-bird", "lexeme-fish"]}'::jsonb),
    ('animals-001', 1, 'step-speak', 'SPEAK', 2, true, '{"sentenceIds": ["sent-i-see-a-cat", "sent-i-see-a-dog", "sent-i-like-rabbits", "sent-its-a-dog", "sent-whats-this"]}'::jsonb),
    ('animals-001', 1, 'step-recognize', 'RECOGNIZE', 3, true, '{"questions": [{"questionId": "q-cat", "correctLexemeId": "lexeme-cat", "optionIds": ["opt-cat", "opt-dog"]}, {"questionId": "q-dog", "correctLexemeId": "lexeme-dog", "optionIds": ["opt-dog", "opt-rabbit", "opt-bird"]}, {"questionId": "q-rabbit", "correctLexemeId": "lexeme-rabbit", "optionIds": ["opt-rabbit", "opt-cat"]}, {"questionId": "q-bird", "correctLexemeId": "lexeme-bird", "optionIds": ["opt-bird", "opt-fish", "opt-dog"]}, {"questionId": "q-fish", "correctLexemeId": "lexeme-fish", "optionIds": ["opt-fish", "opt-rabbit"]}]}'::jsonb),
    ('animals-001', 1, 'step-read', 'READ', 4, false, '{"sentenceIds": ["sent-i-see-a-cat", "sent-i-see-a-dog", "sent-i-like-rabbits", "sent-its-a-dog", "sent-whats-this"]}'::jsonb);

INSERT INTO lesson_lexeme (
    lesson_id, lesson_version, lexeme_id, image_asset_id, audio_asset_id, sort_order
) VALUES
    ('animals-001', 1, 'lexeme-cat', 'image-animals-cat', 'audio-animals-cat', 1),
    ('animals-001', 1, 'lexeme-dog', 'image-animals-dog', 'audio-animals-dog', 2),
    ('animals-001', 1, 'lexeme-rabbit', 'image-animals-rabbit', 'audio-animals-rabbit', 3),
    ('animals-001', 1, 'lexeme-bird', 'image-animals-bird', 'audio-animals-bird', 4),
    ('animals-001', 1, 'lexeme-fish', 'image-animals-fish', 'audio-animals-fish', 5);

INSERT INTO lesson_sentence (
    lesson_id, lesson_version, sentence_id, english, chinese, audio_asset_id, sort_order
) VALUES
    ('animals-001', 1, 'sent-i-see-a-cat', $txt$I see a cat.$txt$, $txt$我看见一只猫。$txt$, 'audio-animals-i-see-a-cat', 1),
    ('animals-001', 1, 'sent-i-see-a-dog', $txt$I see a dog.$txt$, $txt$我看见一只狗。$txt$, 'audio-animals-i-see-a-dog', 2),
    ('animals-001', 1, 'sent-i-like-rabbits', $txt$I like rabbits.$txt$, $txt$我喜欢兔子。$txt$, 'audio-animals-i-like-rabbits', 3),
    ('animals-001', 1, 'sent-its-a-dog', $txt$It's a dog.$txt$, $txt$这是一只狗。$txt$, 'audio-animals-its-a-dog', 4),
    ('animals-001', 1, 'sent-whats-this', $txt$What's this?$txt$, $txt$这是什么？$txt$, 'audio-animals-whats-this', 5);

INSERT INTO lesson_step_sentence (
    lesson_id, lesson_version, step_id, sentence_id, sort_order
) VALUES
    ('animals-001', 1, 'step-speak', 'sent-i-see-a-cat', 1),
    ('animals-001', 1, 'step-speak', 'sent-i-see-a-dog', 2),
    ('animals-001', 1, 'step-speak', 'sent-i-like-rabbits', 3),
    ('animals-001', 1, 'step-speak', 'sent-its-a-dog', 4),
    ('animals-001', 1, 'step-speak', 'sent-whats-this', 5),
    ('animals-001', 1, 'step-read', 'sent-i-see-a-cat', 1),
    ('animals-001', 1, 'step-read', 'sent-i-see-a-dog', 2),
    ('animals-001', 1, 'step-read', 'sent-i-like-rabbits', 3),
    ('animals-001', 1, 'step-read', 'sent-its-a-dog', 4),
    ('animals-001', 1, 'step-read', 'sent-whats-this', 5);

INSERT INTO lesson_option (lesson_id, lesson_version, option_id, lexeme_id) VALUES
    ('animals-001', 1, 'opt-cat', 'lexeme-cat'),
    ('animals-001', 1, 'opt-dog', 'lexeme-dog'),
    ('animals-001', 1, 'opt-rabbit', 'lexeme-rabbit'),
    ('animals-001', 1, 'opt-bird', 'lexeme-bird'),
    ('animals-001', 1, 'opt-fish', 'lexeme-fish');

INSERT INTO lesson_question (
    lesson_id, lesson_version, question_id, step_id, correct_lexeme_id, sort_order
) VALUES
    ('animals-001', 1, 'q-cat', 'step-recognize', 'lexeme-cat', 1),
    ('animals-001', 1, 'q-dog', 'step-recognize', 'lexeme-dog', 2),
    ('animals-001', 1, 'q-rabbit', 'step-recognize', 'lexeme-rabbit', 3),
    ('animals-001', 1, 'q-bird', 'step-recognize', 'lexeme-bird', 4),
    ('animals-001', 1, 'q-fish', 'step-recognize', 'lexeme-fish', 5);

INSERT INTO lesson_question_option (
    lesson_id, lesson_version, question_id, option_id, sort_order
) VALUES
    ('animals-001', 1, 'q-cat', 'opt-cat', 1),
    ('animals-001', 1, 'q-cat', 'opt-dog', 2),
    ('animals-001', 1, 'q-dog', 'opt-dog', 1),
    ('animals-001', 1, 'q-dog', 'opt-rabbit', 2),
    ('animals-001', 1, 'q-dog', 'opt-bird', 3),
    ('animals-001', 1, 'q-rabbit', 'opt-rabbit', 1),
    ('animals-001', 1, 'q-rabbit', 'opt-cat', 2),
    ('animals-001', 1, 'q-bird', 'opt-bird', 1),
    ('animals-001', 1, 'q-bird', 'opt-fish', 2),
    ('animals-001', 1, 'q-bird', 'opt-dog', 3),
    ('animals-001', 1, 'q-fish', 'opt-fish', 1),
    ('animals-001', 1, 'q-fish', 'opt-rabbit', 2);
