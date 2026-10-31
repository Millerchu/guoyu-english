package com.guoyu.english.auth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.guoyu.english.auth.infrastructure.SecretGenerator;
import com.guoyu.english.common.security.Digests;
import com.guoyu.english.common.web.RequestIds;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 从空库走完引导、建档、配对和撤销。断言不把 PIN、引导码或令牌写进失败信息。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OnboardingFlowTest {

    private static final String BOOTSTRAP_CODE = UUID.randomUUID() + "-" + UUID.randomUUID();

    private static final String PIN = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));

    private static final String OTHER_PIN = bump(PIN);

    private static final Path SECRET_FILE = Path.of("/tmp/s06-secrets.txt");

    private static final String TRUNCATE = """
            TRUNCATE TABLE
                device_child_binding,
                client_device,
                pair_code_child,
                pair_code,
                request_idempotency,
                child_profile,
                parent_session,
                pin_attempt,
                parent_credential,
                family_account
            RESTART IDENTITY CASCADE
            """;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("guoyu.auth.bootstrap-code", () -> BOOTSTRAP_CODE);
    }

    @BeforeAll
    static void rememberSecrets() throws Exception {
        Files.writeString(SECRET_FILE, BOOTSTRAP_CODE + "\n" + PIN + "\n" + OTHER_PIN + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @BeforeEach
    void emptyDatabase() {
        jdbcTemplate.execute(TRUNCATE);
    }

    @Test
    void migratesV2CourseThenV3Identity() throws Exception {
        List<Map<String, Object>> versions = jdbcTemplate.queryForList(
                "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank");
        assertThat(versions).extracting(row -> row.get("version").toString()).containsExactly("1", "2", "3");
        assertThat(versions).allMatch(row -> Boolean.TRUE.equals(row.get("success")));

        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name",
                String.class);
        assertThat(tables).contains(
                "lesson",
                "course_theme",
                "family_account",
                "parent_credential",
                "pin_attempt",
                "parent_session",
                "request_idempotency",
                "child_profile",
                "pair_code",
                "pair_code_child",
                "client_device",
                "device_child_binding");
        assertThat(tables).doesNotContain(
                "daily_assignment",
                "learning_event",
                "star_ledger",
                "sync_change_log");

        String timezoneDefault = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns "
                        + "WHERE table_name = 'child_profile' AND column_name = 'timezone_id'",
                String.class);
        assertThat(timezoneDefault).contains("Asia/Shanghai");
        String issuedType = jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns "
                        + "WHERE table_name = 'child_profile' AND column_name = 'issued_through'",
                String.class);
        assertThat(issuedType).isEqualTo("date");
    }

    @Test
    void completesBootstrapChildPairAndRevoke() throws Exception {
        try (LogCapture logs = new LogCapture()) {
            assertThat(count("family_account")).isZero();
            JsonNode badPin = envelope(post("/api/v1/parent/bootstrap", Map.of(
                    "bootstrapCode", BOOTSTRAP_CODE,
                    "newPin", PIN.substring(0, 3)),
                    "Idempotency-Key", UUID.randomUUID().toString()),
                    HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT");
            assertThat(badPin.get("data").isNull()).isTrue();
            assertThat(count("family_account")).isZero();

            envelope(post("/api/v1/parent/bootstrap", Map.of(
                    "bootstrapCode", UUID.randomUUID().toString(),
                    "newPin", PIN),
                    "Idempotency-Key", UUID.randomUUID().toString()),
                    HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT");
            assertThat(count("family_account")).isZero();

            String bootstrapKey = UUID.randomUUID().toString();
            String requestId = "s06-" + UUID.randomUUID();
            JsonNode created = envelope(post("/api/v1/parent/bootstrap", Map.of(
                    "bootstrapCode", BOOTSTRAP_CODE,
                    "newPin", PIN),
                    "Idempotency-Key", bootstrapKey,
                    RequestIds.HEADER, requestId),
                    HttpStatus.OK, "OK");
            assertThat(created.get("requestId").asText()).isEqualTo(requestId);
            long familyId = created.get("data").get("familyId").asLong();
            String parentToken = created.get("data").get("parentToken").asText();
            remember(parentToken);
            assertThat(parentToken).startsWith(SecretGenerator.PARENT_PREFIX);
            assertExpiry(created.get("data").get("expiresAt").asText(), 15);
            assertThat(count("family_account")).isEqualTo(1L);

            JsonNode replay = envelope(post("/api/v1/parent/bootstrap", Map.of(
                    "bootstrapCode", BOOTSTRAP_CODE,
                    "newPin", PIN),
                    "Idempotency-Key", bootstrapKey),
                    HttpStatus.OK, "OK");
            assertThat(replay.get("data").get("familyId").asLong()).isEqualTo(familyId);
            String replayToken = replay.get("data").get("parentToken").asText();
            remember(replayToken);
            assertThat(replayToken).isNotEqualTo(parentToken);
            assertThat(count("family_account")).isEqualTo(1L);

            envelope(post("/api/v1/parent/bootstrap", Map.of(
                    "bootstrapCode", BOOTSTRAP_CODE,
                    "newPin", OTHER_PIN),
                    "Idempotency-Key", bootstrapKey),
                    HttpStatus.CONFLICT, "EVENT_ID_REUSED");
            assertThat(count("family_account")).isEqualTo(1L);

            envelope(post("/api/v1/parent/bootstrap", Map.of(
                    "bootstrapCode", BOOTSTRAP_CODE,
                    "newPin", PIN),
                    "Idempotency-Key", UUID.randomUUID().toString()),
                    HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT");
            assertThat(count("family_account")).isEqualTo(1L);

            JsonNode emptyChildren = envelope(get("/api/v1/parent/children", parentToken), HttpStatus.OK, "OK");
            assertThat(emptyChildren.get("data").isArray()).isTrue();
            assertThat(emptyChildren.get("data")).isEmpty();

            String childKey = UUID.randomUUID().toString();
            envelope(post("/api/v1/parent/children", Map.of(
                    "nickname", "名".repeat(21),
                    "ageBand", "AGE_6"),
                    "Authorization", bearer(parentToken),
                    "Idempotency-Key", childKey),
                    HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT");
            assertThat(count("child_profile")).isZero();

            JsonNode child = envelope(post("/api/v1/parent/children", Map.of(
                    "nickname", "小球",
                    "ageBand", "AGE_6",
                    "dailyMinutes", 20,
                    "timezoneId", "Asia/Shanghai"),
                    "Authorization", bearer(parentToken),
                    "Idempotency-Key", childKey),
                    HttpStatus.OK, "OK");
            long childId = child.get("data").get("childId").asLong();
            assertThat(child.get("data").get("nickname").asText()).isEqualTo("小球");
            assertThat(child.get("data").get("dailyMinutes").asInt()).isEqualTo(20);
            assertThat(child.get("data").get("profileVersion").asInt()).isEqualTo(1);
            assertThat(child.get("data").get("timezoneVersion").asInt()).isEqualTo(1);
            assertThat(child.get("data").get("issuedThrough").isNull()).isTrue();

            JsonNode sameChild = envelope(post("/api/v1/parent/children", Map.of(
                    "nickname", "小球",
                    "ageBand", "AGE_6",
                    "dailyMinutes", 20,
                    "timezoneId", "Asia/Shanghai"),
                    "Authorization", bearer(parentToken),
                    "Idempotency-Key", childKey),
                    HttpStatus.OK, "OK");
            assertThat(sameChild.get("data").get("childId").asLong()).isEqualTo(childId);
            assertThat(count("child_profile")).isEqualTo(1L);

            envelope(post("/api/v1/parent/children", Map.of(
                    "nickname", "小石",
                    "ageBand", "AGE_6",
                    "dailyMinutes", 20,
                    "timezoneId", "Asia/Shanghai"),
                    "Authorization", bearer(parentToken),
                    "Idempotency-Key", childKey),
                    HttpStatus.CONFLICT, "EVENT_ID_REUSED");
            assertThat(count("child_profile")).isEqualTo(1L);

            JsonNode defaults = envelope(post("/api/v1/parent/children", Map.of(
                    "nickname", "小石",
                    "ageBand", "AGE_5"),
                    "Authorization", bearer(replayToken),
                    "Idempotency-Key", UUID.randomUUID().toString()),
                    HttpStatus.OK, "OK");
            assertThat(defaults.get("data").get("dailyMinutes").asInt()).isEqualTo(15);
            assertThat(defaults.get("data").get("timezoneId").asText()).isEqualTo("Asia/Shanghai");
            assertThat(defaults.get("data").get("issuedThrough").isNull()).isTrue();
            long secondChildId = defaults.get("data").get("childId").asLong();

            JsonNode listed = envelope(get("/api/v1/parent/children", parentToken), HttpStatus.OK, "OK");
            assertThat(listed.get("data")).hasSize(2);

            JsonNode pairCode = envelope(post("/api/v1/parent/pair-codes", Map.of(
                    "deviceName", "客厅平板",
                    "childIds", List.of(childId, secondChildId)),
                    "Authorization", bearer(parentToken)),
                    HttpStatus.OK, "OK");
            String code = pairCode.get("data").get("pairCode").asText();
            remember(code);
            assertPairCode(code);
            assertExpiry(pairCode.get("data").get("expiresAt").asText(), 5);

            String deviceId = UUID.randomUUID().toString();
            Map<String, Object> pairBody = new LinkedHashMap<>();
            pairBody.put("pairCode", code);
            pairBody.put("deviceId", deviceId);
            pairBody.put("platform", "H5");
            pairBody.put("pin", PIN);
            envelope(post("/api/v1/devices/pair", pairBody), HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT");
            assertThat(count("client_device")).isZero();
            assertThat(countWhere("pair_code", "consumed_at IS NULL")).isEqualTo(1L);

            pairBody.remove("pin");
            JsonNode paired = envelope(post("/api/v1/devices/pair", pairBody), HttpStatus.OK, "OK");
            String deviceToken = paired.get("data").get("deviceToken").asText();
            remember(deviceToken);
            assertThat(deviceToken).startsWith(SecretGenerator.DEVICE_PREFIX);
            assertThat(paired.get("data").get("deviceId").asText()).isEqualTo(deviceId);
            assertThat(paired.get("data").get("childIds")).hasSize(2);
            envelope(post("/api/v1/devices/pair", pairBody), HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT");

            envelope(post("/api/v1/parent/sessions", Map.of("pin", OTHER_PIN),
                    "Authorization", bearer(deviceToken)),
                    HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT");
            assertThat(failedCount()).isEqualTo(1);
            assertThat(lockedUntil()).isNull();

            JsonNode session = envelope(post("/api/v1/parent/sessions", Map.of("pin", PIN),
                    "Authorization", bearer(deviceToken)),
                    HttpStatus.OK, "OK");
            String deviceParentToken = session.get("data").get("parentToken").asText();
            remember(deviceParentToken);
            assertThat(deviceParentToken).startsWith(SecretGenerator.PARENT_PREFIX);
            assertThat(failedCount()).isZero();
            assertExpiry(session.get("data").get("expiresAt").asText(), 15);

            JsonNode devices = envelope(get("/api/v1/parent/devices", parentToken), HttpStatus.OK, "OK");
            assertThat(devices.get("data")).hasSize(1);
            assertThat(devices.get("data").get(0).get("revoked").asBoolean()).isFalse();
            assertThat(devices.get("data").get(0).get("platform").asText()).isEqualTo("H5");
            assertThat(devices.get("data").get(0).has("deviceToken")).isFalse();

            JsonNode revoked = envelope(delete("/api/v1/parent/devices/" + deviceId, deviceParentToken),
                    HttpStatus.OK, "OK");
            assertThat(revoked.get("data").get("revoked").asBoolean()).isTrue();
            JsonNode revokedAgain = envelope(delete("/api/v1/parent/devices/" + deviceId, parentToken),
                    HttpStatus.OK, "OK");
            assertThat(revokedAgain.get("data").get("revoked").asBoolean()).isTrue();

            envelope(post("/api/v1/parent/sessions", Map.of("pin", PIN),
                    "Authorization", bearer(deviceToken)),
                    HttpStatus.UNAUTHORIZED, "DEVICE_REVOKED");
            envelope(get("/api/v1/parent/children", deviceParentToken),
                    HttpStatus.UNAUTHORIZED, "DEVICE_REVOKED");
            JsonNode stillThere = envelope(get("/api/v1/parent/children", parentToken), HttpStatus.OK, "OK");
            assertThat(stillThere.get("data")).hasSize(2);

            JsonNode nextCode = envelope(post("/api/v1/parent/pair-codes", Map.of(
                    "childIds", List.of(childId)),
                    "Authorization", bearer(parentToken)),
                    HttpStatus.OK, "OK");
            String secondCode = nextCode.get("data").get("pairCode").asText();
            remember(secondCode);
            JsonNode repaired = envelope(post("/api/v1/devices/pair", Map.of(
                    "pairCode", secondCode,
                    "deviceId", deviceId,
                    "platform", "ANDROID")),
                    HttpStatus.OK, "OK");
            String newDeviceToken = repaired.get("data").get("deviceToken").asText();
            remember(newDeviceToken);
            assertThat(newDeviceToken).isNotEqualTo(deviceToken);
            envelope(post("/api/v1/parent/sessions", Map.of("pin", PIN),
                    "Authorization", bearer(deviceToken)),
                    HttpStatus.UNAUTHORIZED, "DEVICE_REVOKED");

            assertStoredSecrets(parentToken, deviceToken, newDeviceToken, code);
            SecretText.assertAbsent(logs.text(), PIN);
            SecretText.assertAbsent(logs.text(), OTHER_PIN);
            SecretText.assertAbsent(logs.text(), BOOTSTRAP_CODE);
            SecretText.assertAbsent(logs.text(), parentToken);
            SecretText.assertAbsent(logs.text(), replayToken);
            SecretText.assertAbsent(logs.text(), deviceParentToken);
            SecretText.assertAbsent(logs.text(), deviceToken);
            SecretText.assertAbsent(logs.text(), newDeviceToken);
            SecretText.assertAbsent(logs.text(), code);
            SecretText.assertAbsent(logs.text(), secondCode);
        }
    }

    private void assertStoredSecrets(String parentToken, String oldDeviceToken, String activeDeviceToken, String pairCode) {
        String pinHash = jdbcTemplate.queryForObject("SELECT pin_hash FROM parent_credential", String.class);
        assertThat(pinHash).startsWith("$argon2id$");
        assertThat(pinHash).contains("m=65536,t=3,p=1");
        String activeHash = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM client_device WHERE revoked = FALSE", String.class);
        assertThat(activeHash).isEqualTo(Digests.sha256Hex(activeDeviceToken)).hasSize(64);
        Long staleDevice = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM client_device WHERE token_hash = ?",
                Long.class, Digests.sha256Hex(oldDeviceToken));
        assertThat(staleDevice).isZero();
        Long parentHashRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM parent_session WHERE token_hash = ?",
                Long.class, Digests.sha256Hex(parentToken));
        assertThat(parentHashRows).isEqualTo(1L);
        Long rawCode = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pair_code WHERE code_hash = ?", Long.class, pairCode);
        assertThat(rawCode).isZero();
        Long hashedCode = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pair_code WHERE code_hash = ?", Long.class, Digests.sha256Hex(pairCode));
        assertThat(hashedCode).isEqualTo(1L);
        assertThat(count("device_child_binding")).isEqualTo(1L);

        List<String> stored = jdbcTemplate.queryForList("""
                SELECT pin_hash FROM parent_credential
                UNION ALL SELECT token_hash FROM parent_session
                UNION ALL SELECT request_hash FROM request_idempotency
                UNION ALL SELECT code_hash FROM pair_code
                UNION ALL SELECT COALESCE(device_name, '') FROM pair_code
                UNION ALL SELECT token_hash FROM client_device
                UNION ALL SELECT COALESCE(display_name, '') FROM client_device
                """, String.class);
        String joined = String.join("\n", stored);
        SecretText.assertAbsent(joined, PIN);
        SecretText.assertAbsent(joined, OTHER_PIN);
        SecretText.assertAbsent(joined, BOOTSTRAP_CODE);
        SecretText.assertAbsent(joined, parentToken);
        SecretText.assertAbsent(joined, oldDeviceToken);
        SecretText.assertAbsent(joined, activeDeviceToken);
        SecretText.assertAbsent(joined, pairCode);
    }

    private ResponseEntity<String> post(String path, Object body, String... headers) {
        return exchange(HttpMethod.POST, path, body, headers);
    }

    private ResponseEntity<String> get(String path, String parentToken) {
        return exchange(HttpMethod.GET, path, null, "Authorization", bearer(parentToken));
    }

    private ResponseEntity<String> delete(String path, String parentToken) {
        return exchange(HttpMethod.DELETE, path, null, "Authorization", bearer(parentToken));
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, Object body, String... headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        for (int i = 0; i + 1 < headers.length; i += 2) {
            httpHeaders.set(headers[i], headers[i + 1]);
        }
        return restTemplate.exchange(path, method, new HttpEntity<>(body, httpHeaders), String.class);
    }

    private JsonNode envelope(ResponseEntity<String> response, HttpStatus status, String code) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(status);
        MediaType contentType = response.getHeaders().getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.toString().toLowerCase()).contains("application/json");
        JsonNode root = objectMapper.readTree(response.getBody());
        Set<String> fields = new HashSet<>();
        root.fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("requestId", "code", "message", "data", "serverTime");
        assertThat(root.get("code").asText()).isEqualTo(code);
        assertThat(root.get("message").asText()).isEqualTo(code);
        assertThat(root.get("serverTime").asText()).endsWith("Z");
        Instant.parse(root.get("serverTime").asText());
        assertThat(response.getHeaders().getFirst(RequestIds.HEADER)).isEqualTo(root.get("requestId").asText());
        return root;
    }

    private void assertExpiry(String text, int minutes) {
        Instant expiresAt = Instant.parse(text);
        assertThat(text).endsWith("Z");
        assertThat(expiresAt).isAfter(Instant.now().plus(minutes - 1L, ChronoUnit.MINUTES));
        assertThat(expiresAt).isBefore(Instant.now().plus(minutes + 1L, ChronoUnit.MINUTES));
    }

    private void assertPairCode(String code) {
        if (code.length() != SecretGenerator.PAIR_CODE_LENGTH) {
            throw new AssertionError("pair code length");
        }
        for (int i = 0; i < code.length(); i++) {
            if (SecretGenerator.PAIR_ALPHABET.indexOf(code.charAt(i)) < 0) {
                throw new AssertionError("pair code alphabet");
            }
        }
    }

    private long count(String table) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return count == null ? 0L : count;
    }

    private long countWhere(String table, String where) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + where, Long.class);
        return count == null ? 0L : count;
    }

    private int failedCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT failed_count FROM pin_attempt", Integer.class);
        return count == null ? 0 : count;
    }

    private Instant lockedUntil() {
        Object value = jdbcTemplate.queryForObject("SELECT locked_until FROM pin_attempt", Object.class);
        return toInstant(value);
    }

    private Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        throw new AssertionError("unexpected time type");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private static void remember(String secret) throws Exception {
        Files.writeString(SECRET_FILE, secret + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String bump(String pin) {
        int value = Integer.parseInt(pin);
        return String.format("%06d", (value + 1) % 1_000_000);
    }
}
