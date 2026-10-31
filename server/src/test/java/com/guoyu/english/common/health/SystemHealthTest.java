package com.guoyu.english.common.health;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.ibatis.session.SqlSessionFactory;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在 postgres:16 上验收 Flyway 与健康检查。V2 起有课程表，健康检查本身不返回这些表。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SystemHealthTest {

    private static final String HEALTH_PATH = "/api/v1/system/health";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void flywayAppliesV1AndV2OnPostgres16() throws Exception {
        assertThat(POSTGRES.getDockerImageName()).contains("postgres:16");
        assertThat(sqlSessionFactory).isNotNull();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            try (ResultSet version = statement.executeQuery("SHOW server_version")) {
                assertThat(version.next()).isTrue();
                assertThat(version.getString(1)).startsWith("16");
            }
            try (ResultSet history = statement.executeQuery(
                    "SELECT version, script, success FROM flyway_schema_history ORDER BY installed_rank")) {
                List<String> versions = new ArrayList<>();
                List<String> scripts = new ArrayList<>();
                while (history.next()) {
                    versions.add(history.getString("version"));
                    scripts.add(history.getString("script"));
                    assertThat(history.getBoolean("success")).isTrue();
                }
                assertThat(versions).containsExactly("1", "2");
                assertThat(scripts).containsExactly("V1__baseline.sql", "V2__course_media.sql");
            }
            try (ResultSet tables = statement.executeQuery(
                    "SELECT table_name FROM information_schema.tables "
                            + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' "
                            + "ORDER BY table_name")) {
                Set<String> names = new HashSet<>();
                while (tables.next()) {
                    names.add(tables.getString("table_name"));
                }
                assertThat(names).containsExactlyInAnyOrder(
                        "flyway_schema_history",
                        "course_theme",
                        "media_asset",
                        "lexeme",
                        "lesson",
                        "lesson_step",
                        "lesson_lexeme",
                        "lesson_sentence",
                        "lesson_step_sentence",
                        "lesson_question",
                        "lesson_option",
                        "lesson_question_option");
                assertThat(names).doesNotContain(
                        "child_profile",
                        "client_device",
                        "learning_event",
                        "star_ledger",
                        "daily_assignment");
            }
        }
    }

    @Test
    void healthReturnsEnvelopeWhenRequestIdMissing() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(HEALTH_PATH, String.class);
        JsonNode root = assertHealthy(response);
        UUID.fromString(root.get("requestId").asText());
        assertThat(response.getHeaders().getFirst("X-Request-Id")).isEqualTo(root.get("requestId").asText());
    }

    @Test
    void healthEchoesRequestIdAndHidesSecrets() throws Exception {
        String requestId = "11111111-2222-4333-8444-555555555555";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", requestId);
        ResponseEntity<String> response = restTemplate.exchange(
                HEALTH_PATH, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        JsonNode root = assertHealthy(response);
        assertThat(root.get("requestId").asText()).isEqualTo(requestId);

        String body = response.getBody();
        assertThat(body).doesNotContain(POSTGRES.getPassword());
        assertThat(body).doesNotContain(POSTGRES.getJdbcUrl());
        assertThat(body).doesNotContain("MEDIA_ROOT");
        assertThat(body.toLowerCase()).doesNotContain("token");
        assertThat(body.toLowerCase()).doesNotContain("password");
    }

    private JsonNode assertHealthy(ResponseEntity<String> response) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MediaType contentType = response.getHeaders().getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.toString().toLowerCase()).contains("application/json").contains("utf-8");

        JsonNode root = objectMapper.readTree(response.getBody());
        Set<String> fields = new HashSet<>();
        root.fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("requestId", "code", "message", "data", "serverTime");
        assertThat(root.get("code").asText()).isEqualTo("OK");
        assertThat(root.get("message").asText()).isEqualTo("OK");
        assertThat(root.get("requestId").asText()).isNotBlank();

        String serverTimeText = root.get("serverTime").asText();
        Instant serverTime = Instant.parse(serverTimeText);
        assertThat(serverTimeText).endsWith("Z");
        assertThat(serverTime).isBetween(
                Instant.now().minus(2, ChronoUnit.MINUTES),
                Instant.now().plus(2, ChronoUnit.MINUTES));

        JsonNode data = root.get("data");
        Set<String> dataFields = new HashSet<>();
        data.fieldNames().forEachRemaining(dataFields::add);
        assertThat(dataFields).containsExactlyInAnyOrder("status", "version");
        assertThat(data.get("status").asText()).isEqualTo("UP");
        assertThat(data.get("version").asText()).isEqualTo("0.0.1-SNAPSHOT");
        assertThat(data.get("version").asText()).doesNotContain("://");
        assertThat(data.has("ready")).isFalse();
        return root;
    }
}
