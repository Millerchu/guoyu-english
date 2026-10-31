package com.guoyu.english.auth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.guoyu.english.auth.domain.TimeSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * 连续失败 5 次后的锁定时间写在数据库里。时钟由测试推进，避免真的等待。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PinAttemptDelayTest {

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

    @MockitoBean
    private TimeSource timeSource;

    private Instant now;

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
        now = Instant.parse("2026-10-31T00:00:00Z");
        given(timeSource.now()).willAnswer(invocation -> now);
        jdbcTemplate.execute(TRUNCATE);
    }

    @Test
    void persistsDelayAfterFiveFailures() throws Exception {
        try (LogCapture logs = new LogCapture()) {
            String parentToken = bootstrap();
            long childId = createChild(parentToken);
            String deviceToken = pair(parentToken, childId);

            for (int attempt = 1; attempt <= 4; attempt++) {
                session(deviceToken, OTHER_PIN, HttpStatus.BAD_REQUEST);
            }
            assertThat(failedCount()).isEqualTo(4);
            assertThat(lockedUntil()).isNull();

            session(deviceToken, OTHER_PIN, HttpStatus.BAD_REQUEST);
            assertThat(failedCount()).isEqualTo(5);
            assertThat(lockedUntil()).isEqualTo(now.plusSeconds(30));

            now = now.plusSeconds(10);
            session(deviceToken, PIN, HttpStatus.BAD_REQUEST);
            assertThat(failedCount()).isEqualTo(5);
            assertThat(lockedUntil()).isEqualTo(Instant.parse("2026-10-31T00:00:30Z"));

            now = Instant.parse("2026-10-31T00:00:31Z");
            session(deviceToken, PIN, HttpStatus.OK);
            assertThat(failedCount()).isZero();
            assertThat(lockedUntil()).isNull();

            SecretText.assertAbsent(logs.text(), PIN);
            SecretText.assertAbsent(logs.text(), OTHER_PIN);
            SecretText.assertAbsent(logs.text(), BOOTSTRAP_CODE);
            SecretText.assertAbsent(logs.text(), parentToken);
            SecretText.assertAbsent(logs.text(), deviceToken);
        }
    }

    private String bootstrap() throws Exception {
        JsonNode root = read(post("/api/v1/parent/bootstrap", Map.of(
                "bootstrapCode", BOOTSTRAP_CODE,
                "newPin", PIN),
                "Idempotency-Key", UUID.randomUUID().toString()), HttpStatus.OK);
        String token = root.get("data").get("parentToken").asText();
        Files.writeString(SECRET_FILE, token + "\n", StandardOpenOption.APPEND);
        return token;
    }

    private long createChild(String parentToken) throws Exception {
        JsonNode root = read(post("/api/v1/parent/children", Map.of(
                "nickname", "小球",
                "ageBand", "AGE_6"),
                "Authorization", "Bearer " + parentToken,
                "Idempotency-Key", UUID.randomUUID().toString()), HttpStatus.OK);
        return root.get("data").get("childId").asLong();
    }

    private String pair(String parentToken, long childId) throws Exception {
        JsonNode code = read(post("/api/v1/parent/pair-codes", Map.of("childIds", java.util.List.of(childId)),
                "Authorization", "Bearer " + parentToken), HttpStatus.OK);
        String pairCode = code.get("data").get("pairCode").asText();
        Files.writeString(SECRET_FILE, pairCode + "\n", StandardOpenOption.APPEND);
        String deviceId = UUID.randomUUID().toString();
        JsonNode paired = read(post("/api/v1/devices/pair", Map.of(
                "pairCode", pairCode,
                "deviceId", deviceId,
                "platform", "H5")), HttpStatus.OK);
        String token = paired.get("data").get("deviceToken").asText();
        Files.writeString(SECRET_FILE, token + "\n", StandardOpenOption.APPEND);
        return token;
    }

    private void session(String deviceToken, String pin, HttpStatus status) throws Exception {
        read(post("/api/v1/parent/sessions", Map.of("pin", pin),
                "Authorization", "Bearer " + deviceToken), status);
    }

    private ResponseEntity<String> post(String path, Object body, String... headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        for (int i = 0; i + 1 < headers.length; i += 2) {
            httpHeaders.set(headers[i], headers[i + 1]);
        }
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, httpHeaders), String.class);
    }

    private JsonNode read(ResponseEntity<String> response, HttpStatus status) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(status);
        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.get("code").asText()).isEqualTo(status == HttpStatus.OK ? "OK" : "INVALID_ARGUMENT");
        return root;
    }

    private int failedCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT failed_count FROM pin_attempt", Integer.class);
        return count == null ? 0 : count;
    }

    private Instant lockedUntil() {
        Object value = jdbcTemplate.queryForObject("SELECT locked_until FROM pin_attempt", Object.class);
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

    private static String bump(String pin) {
        int value = Integer.parseInt(pin);
        return String.format("%06d", (value + 1) % 1_000_000);
    }
}
