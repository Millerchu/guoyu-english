package com.guoyu.english.course;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.guoyu.english.support.RepoPaths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 在 postgres:16 上验收 V2 种子和动物课查询。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class LessonQueryTest {

    private static final String LESSON_PATH = "/api/v1/lessons/animals-001?version=1";

    private static final List<String> LEXEME_IDS = List.of(
            "lexeme-cat", "lexeme-dog", "lexeme-rabbit", "lexeme-bird", "lexeme-fish");

    private static final List<String> ENGLISH = List.of("cat", "dog", "rabbit", "bird", "fish");

    private static final List<String> CHINESE = List.of("猫", "狗", "兔子", "鸟", "鱼");

    private static final List<String> SENTENCE_IDS = List.of(
            "sent-i-see-a-cat",
            "sent-i-see-a-dog",
            "sent-i-like-rabbits",
            "sent-its-a-dog",
            "sent-whats-this");

    private static final List<String> SENTENCE_ENGLISH = List.of(
            "I see a cat.",
            "I see a dog.",
            "I like rabbits.",
            "It's a dog.",
            "What's this?");

    private static final List<String> SENTENCE_CHINESE = List.of(
            "我看见一只猫。",
            "我看见一只狗。",
            "我喜欢兔子。",
            "这是一只狗。",
            "这是什么？");

    private static final Map<String, List<String>> QUESTION_OPTIONS = new LinkedHashMap<>();

    static {
        QUESTION_OPTIONS.put("q-cat", List.of("opt-cat", "opt-dog"));
        QUESTION_OPTIONS.put("q-dog", List.of("opt-dog", "opt-rabbit", "opt-bird"));
        QUESTION_OPTIONS.put("q-rabbit", List.of("opt-rabbit", "opt-cat"));
        QUESTION_OPTIONS.put("q-bird", List.of("opt-bird", "opt-fish", "opt-dog"));
        QUESTION_OPTIONS.put("q-fish", List.of("opt-fish", "opt-rabbit"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void seedHasFiveWordsFiveSentencesAndTwoOrThreeOptions() throws Exception {
        assertThat(POSTGRES.getDockerImageName()).contains("postgres:16");
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            assertThat(count(statement, "SELECT count(*) FROM course_theme")).isEqualTo(1);
            assertThat(count(statement, "SELECT count(*) FROM lesson")).isEqualTo(1);
            assertThat(count(statement, "SELECT count(*) FROM lesson_step")).isEqualTo(4);
            assertThat(count(statement, "SELECT count(*) FROM lexeme")).isEqualTo(5);
            assertThat(count(statement, "SELECT count(*) FROM lesson_sentence")).isEqualTo(5);
            assertThat(count(statement, "SELECT count(*) FROM lesson_question")).isEqualTo(5);
            assertThat(count(statement, "SELECT count(*) FROM lesson_option")).isEqualTo(5);
            assertThat(count(statement, "SELECT count(*) FROM media_asset")).isEqualTo(15);
            try (ResultSet options = statement.executeQuery(
                    "SELECT question_id, count(*) AS option_count "
                            + "FROM lesson_question_option GROUP BY question_id ORDER BY question_id")) {
                int questions = 0;
                while (options.next()) {
                    questions++;
                    int optionCount = options.getInt("option_count");
                    assertThat(optionCount).isBetween(2, 3);
                }
                assertThat(questions).isEqualTo(5);
            }
            try (ResultSet children = statement.executeQuery(
                    "SELECT table_name FROM information_schema.tables "
                            + "WHERE table_schema = 'public' AND table_name IN "
                            + "('child_profile', 'client_device', 'learning_event', 'star_ledger')")) {
                assertThat(children.next()).isFalse();
            }
        }
    }

    @Test
    void animalsLessonReturnsTheContractInsideTheEnvelope() throws Exception {
        String requestId = "22222222-3333-4444-8555-666666666666";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", requestId);
        ResponseEntity<String> response = restTemplate.exchange(
                LESSON_PATH, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MediaType contentType = response.getHeaders().getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.toString().toLowerCase()).contains("application/json").contains("utf-8");
        assertThat(response.getHeaders().getFirst("X-Request-Id")).isEqualTo(requestId);

        JsonNode root = objectMapper.readTree(response.getBody());
        Set<String> fields = fieldNames(root);
        assertThat(fields).containsExactlyInAnyOrder("requestId", "code", "message", "data", "serverTime");
        assertThat(root.get("requestId").asText()).isEqualTo(requestId);
        assertThat(root.get("code").asText()).isEqualTo("OK");
        assertThat(root.get("message").asText()).isEqualTo("OK");
        Instant serverTime = Instant.parse(root.get("serverTime").asText());
        assertThat(root.get("serverTime").asText()).endsWith("Z");
        assertThat(serverTime).isBetween(
                Instant.now().minus(2, ChronoUnit.MINUTES),
                Instant.now().plus(2, ChronoUnit.MINUTES));

        JsonNode data = root.get("data");
        assertThat(data.get("lessonId").asText()).isEqualTo("animals-001");
        assertThat(data.get("version").asInt()).isEqualTo(1);
        assertThat(data.get("themeId").asText()).isEqualTo("theme-animals");
        assertThat(data.get("themeNameZh").asText()).isEqualTo("动物");
        assertThat(data.get("themeNameEn").asText()).isEqualTo("Animals");
        assertLessonContent(data);
        assertMediaBytes(data);
        assertDefinitionJsonMatches(data);

        String body = response.getBody();
        assertThat(body).doesNotContain(POSTGRES.getPassword());
        assertThat(body).doesNotContain(POSTGRES.getJdbcUrl());
        assertThat(body).doesNotContain("/data/media");
        assertThat(body.toLowerCase()).doesNotContain("password");
        assertThat(body).doesNotContain("PIN");
    }

    @Test
    void unknownLessonOrVersionIsInvalidArgument() throws Exception {
        assertInvalid("/api/v1/lessons/animals-001?version=3");
        assertInvalid("/api/v1/lessons/animals-001?version=0");
        assertInvalid("/api/v1/lessons/animals-001?version=-1");
        assertInvalid("/api/v1/lessons/missing-001?version=1");
        assertInvalid("/api/v1/lessons/Animals-001?version=1");
        assertInvalid("/api/v1/lessons/animals-001?version=abc");
        assertInvalid("/api/v1/lessons/animals-001");
        ResponseEntity<String> versionThree = restTemplate.getForEntity(
                "/api/v1/lessons/animals-001?version=3", String.class);
        assertThat(versionThree.getBody()).doesNotContain("lexeme-cat");
        assertThat(versionThree.getBody()).doesNotContain("I see a cat.");
    }

    @Test
    void extraOptionAndAbsoluteStorageKeyAreRejected() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO lesson_question_option "
                            + "(lesson_id, lesson_version, question_id, option_id, sort_order) "
                            + "VALUES ('animals-001', 1, 'q-dog', 'opt-cat', 4)")) {
                assertThat(insert.executeUpdate()).isEqualTo(1);
                assertThatThrownBy(connection::commit)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("question option count must be 2 or 3");
            } finally {
                connection.rollback();
            }
        }
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            assertThatThrownBy(() -> statement.executeUpdate(
                    "INSERT INTO media_asset (asset_id, storage_key, source, mime, size_bytes, sha256) "
                            + "VALUES ('bad-absolute', '/etc/passwd', 'PLACEHOLDER', 'image/svg+xml', 1, "
                            + "'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa')"))
                    .isInstanceOf(SQLException.class);
            assertThat(count(statement, "SELECT count(*) FROM media_asset WHERE asset_id = 'bad-absolute'"))
                    .isZero();
        }
    }

    private void assertLessonContent(JsonNode data) {
        JsonNode lexemes = data.get("lexemes");
        assertThat(lexemes).hasSize(5);
        for (int i = 0; i < 5; i++) {
            JsonNode lexeme = lexemes.get(i);
            assertThat(lexeme.get("lexemeId").asText()).isEqualTo(LEXEME_IDS.get(i));
            assertThat(lexeme.get("english").asText()).isEqualTo(ENGLISH.get(i));
            assertThat(lexeme.get("chinese").asText()).isEqualTo(CHINESE.get(i));
            assertThat(lexeme.get("image").get("storageKey").asText())
                    .isEqualTo("image/animals/" + ENGLISH.get(i) + ".svg");
            assertThat(lexeme.get("image").get("source").asText()).isEqualTo("HOMEMADE");
            assertThat(lexeme.get("image").get("mime").asText()).isEqualTo("image/svg+xml");
            assertThat(lexeme.get("image").has("speechRate")).isFalse();
            assertThat(lexeme.get("image").has("sourceText")).isFalse();
            JsonNode audio = lexeme.get("audio");
            assertThat(audio.get("storageKey").asText())
                    .isEqualTo("audio/animals/" + ENGLISH.get(i) + "-v1.mp3");
            assertAudio(audio, ENGLISH.get(i));
        }

        JsonNode sentences = data.get("sentences");
        assertThat(sentences).hasSize(5);
        for (int i = 0; i < 5; i++) {
            JsonNode sentence = sentences.get(i);
            assertThat(sentence.get("sentenceId").asText()).isEqualTo(SENTENCE_IDS.get(i));
            assertThat(sentence.get("english").asText()).isEqualTo(SENTENCE_ENGLISH.get(i));
            assertThat(sentence.get("chinese").asText()).isEqualTo(SENTENCE_CHINESE.get(i));
            String fileName = SENTENCE_IDS.get(i).substring("sent-".length()) + "-v1.mp3";
            assertThat(sentence.get("audio").get("storageKey").asText())
                    .isEqualTo("audio/animals/" + fileName);
            assertAudio(sentence.get("audio"), SENTENCE_ENGLISH.get(i));
        }

        JsonNode steps = data.get("steps");
        assertThat(steps).hasSize(4);
        assertThat(steps.get(0).get("stepId").asText()).isEqualTo("step-listen");
        assertThat(steps.get(0).get("stepType").asText()).isEqualTo("LISTEN");
        assertThat(steps.get(0).get("required").asBoolean()).isTrue();
        assertThat(textList(steps.get(0).get("lexemeIds"))).containsExactlyElementsOf(LEXEME_IDS);
        assertThat(steps.get(0).has("questions")).isFalse();
        assertThat(steps.get(0).has("sentenceIds")).isFalse();

        assertThat(steps.get(1).get("stepId").asText()).isEqualTo("step-speak");
        assertThat(steps.get(1).get("stepType").asText()).isEqualTo("SPEAK");
        assertThat(steps.get(1).get("required").asBoolean()).isTrue();
        assertThat(textList(steps.get(1).get("sentenceIds"))).containsExactlyElementsOf(SENTENCE_IDS);

        assertThat(steps.get(2).get("stepId").asText()).isEqualTo("step-recognize");
        assertThat(steps.get(2).get("stepType").asText()).isEqualTo("RECOGNIZE");
        assertThat(steps.get(2).get("required").asBoolean()).isTrue();
        assertThat(steps.get(2).has("lexemeIds")).isFalse();
        JsonNode questions = steps.get(2).get("questions");
        assertThat(questions).hasSize(5);
        int index = 0;
        for (Map.Entry<String, List<String>> expected : QUESTION_OPTIONS.entrySet()) {
            JsonNode question = questions.get(index);
            assertThat(fieldNames(question)).containsExactlyInAnyOrder("questionId", "correctLexemeId", "options");
            assertThat(question.get("questionId").asText()).isEqualTo(expected.getKey());
            String animal = expected.getKey().substring("q-".length());
            assertThat(question.get("correctLexemeId").asText()).isEqualTo("lexeme-" + animal);
            assertThat(question.get("options")).hasSize(expected.getValue().size());
            assertThat(question.get("options").size()).isBetween(2, 3);
            List<String> optionIds = new ArrayList<>();
            for (JsonNode option : question.get("options")) {
                assertThat(fieldNames(option)).containsExactlyInAnyOrder("optionId", "lexemeId");
                optionIds.add(option.get("optionId").asText());
                assertThat(option.get("lexemeId").asText())
                        .isEqualTo("lexeme-" + option.get("optionId").asText().substring("opt-".length()));
            }
            assertThat(optionIds).containsExactlyElementsOf(expected.getValue());
            index++;
        }

        assertThat(steps.get(3).get("stepId").asText()).isEqualTo("step-read");
        assertThat(steps.get(3).get("stepType").asText()).isEqualTo("READ");
        assertThat(steps.get(3).get("required").asBoolean()).isFalse();
        assertThat(textList(steps.get(3).get("sentenceIds"))).containsExactlyElementsOf(SENTENCE_IDS);

        List<String> required = new ArrayList<>();
        for (JsonNode step : steps) {
            if (step.get("required").asBoolean()) {
                required.add(step.get("stepId").asText());
            }
            assertThat(step.get("stepType").asText()).isNotEqualTo("GAME");
        }
        assertThat(required).containsExactly("step-listen", "step-speak", "step-recognize");
    }

    private void assertAudio(JsonNode audio, String sourceText) {
        assertThat(audio.get("source").asText()).isEqualTo("OFFLINE_TTS");
        assertThat(audio.get("mime").asText()).isEqualTo("audio/mpeg");
        assertThat(audio.get("sourceText").asText()).isEqualTo(sourceText);
        assertThat(audio.get("speechRate").decimalValue()).isEqualByComparingTo("0.80");
        assertThat(audio.get("voice").asText()).isEqualTo("en-us+m3");
        assertThat(audio.get("style").asText()).isEqualTo("clear-warm");
        assertThat(audio.get("tool").asText()).isEqualTo("espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5");
        assertThat(audio.get("pauseBetweenWordsMs").asInt()).isEqualTo(600);
        assertThat(audio.get("pauseBetweenSentencesMs").asInt()).isEqualTo(900);
        assertThat(audio.get("pauseAfterPromptMs").asInt()).isEqualTo(400);
        assertThat(audio.get("generatedAt").asText()).isEqualTo("2026-10-29T15:05:20Z");
        assertThat(audio.get("storageKey").asText()).doesNotStartWith("/");
    }

    private void assertMediaBytes(JsonNode data) throws Exception {
        JsonNode metadata = objectMapper.readTree(
                Files.readString(RepoPaths.mediaSamples().resolve("audio/animals/metadata.json")));
        Map<String, JsonNode> audioByKey = new LinkedHashMap<>();
        for (JsonNode asset : metadata.get("assets")) {
            audioByKey.put(asset.get("storage_key").asText(), asset);
        }
        String manifest = Files.readString(RepoPaths.mediaSamples().resolve("image/animals/MANIFEST.md"));
        for (JsonNode lexeme : data.get("lexemes")) {
            assertFileMatches(lexeme.get("image"), manifest);
            assertAudioMetadata(lexeme.get("audio"), audioByKey);
        }
        for (JsonNode sentence : data.get("sentences")) {
            assertAudioMetadata(sentence.get("audio"), audioByKey);
        }
        assertThat(audioByKey).hasSize(10);
    }

    private void assertFileMatches(JsonNode media, String manifest) throws Exception {
        String storageKey = media.get("storageKey").asText();
        byte[] bytes = Files.readAllBytes(RepoPaths.mediaSamples().resolve(storageKey));
        String hash = sha256(bytes);
        assertThat(media.get("sha256").asText()).isEqualTo(hash);
        assertThat(media.get("sizeBytes").asLong()).isEqualTo(bytes.length);
        assertThat(manifest).contains("| sha256 | " + hash + " |");
        assertThat(manifest).contains("| path | " + storageKey + " |");
    }

    private void assertAudioMetadata(JsonNode media, Map<String, JsonNode> audioByKey) throws Exception {
        String storageKey = media.get("storageKey").asText();
        JsonNode expected = audioByKey.get(storageKey);
        assertThat(expected).isNotNull();
        byte[] bytes = Files.readAllBytes(RepoPaths.mediaSamples().resolve(storageKey));
        assertThat(media.get("sha256").asText()).isEqualTo(expected.get("sha256").asText()).isEqualTo(sha256(bytes));
        assertThat(media.get("sizeBytes").asLong()).isEqualTo(expected.get("size_bytes").asLong()).isEqualTo(bytes.length);
        assertThat(media.get("sourceText").asText()).isEqualTo(expected.get("source_text").asText());
        assertThat(media.get("source").asText()).isEqualTo(expected.get("source").asText());
    }

    private void assertDefinitionJsonMatches(JsonNode data) throws Exception {
        Map<String, String> definitions = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT step_id, definition_json::text AS definition_json "
                                + "FROM lesson_step WHERE lesson_id = ? AND lesson_version = ? "
                                + "ORDER BY sort_order")) {
            statement.setString(1, "animals-001");
            statement.setInt(2, 1);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    definitions.put(rows.getString("step_id"), rows.getString("definition_json"));
                }
            }
        }
        JsonNode listen = objectMapper.readTree(definitions.get("step-listen"));
        assertThat(textList(listen.get("lexemeIds"))).containsExactlyElementsOf(LEXEME_IDS);
        assertThat(textList(data.get("steps").get(0).get("lexemeIds")))
                .containsExactlyElementsOf(textList(listen.get("lexemeIds")));
        for (String stepId : List.of("step-speak", "step-read")) {
            JsonNode definition = objectMapper.readTree(definitions.get(stepId));
            assertThat(textList(definition.get("sentenceIds"))).containsExactlyElementsOf(SENTENCE_IDS);
        }
        JsonNode recognize = objectMapper.readTree(definitions.get("step-recognize"));
        JsonNode questions = recognize.get("questions");
        assertThat(questions).hasSize(5);
        int index = 0;
        for (Map.Entry<String, List<String>> expected : QUESTION_OPTIONS.entrySet()) {
            JsonNode question = questions.get(index);
            assertThat(question.get("questionId").asText()).isEqualTo(expected.getKey());
            assertThat(question.get("correctLexemeId").asText())
                    .isEqualTo("lexeme-" + expected.getKey().substring(2));
            assertThat(textList(question.get("optionIds"))).containsExactlyElementsOf(expected.getValue());
            index++;
        }
    }

    private void assertInvalid(String path) throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(fieldNames(root)).containsExactlyInAnyOrder(
                "requestId", "code", "message", "data", "serverTime");
        assertThat(root.get("code").asText()).isEqualTo("INVALID_ARGUMENT");
        assertThat(root.get("message").asText()).isEqualTo("INVALID_ARGUMENT");
        assertThat(root.get("data").isNull()).isTrue();
        assertThat(root.get("serverTime").asText()).endsWith("Z");
    }

    private static int count(Statement statement, String sql) throws SQLException {
        try (ResultSet result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getInt(1);
        }
    }

    private static List<String> textList(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(item -> values.add(item.asText()));
        return values;
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new HashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
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
