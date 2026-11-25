package com.guoyu.english.sync;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.springframework.transaction.support.TransactionTemplate;
import com.guoyu.english.auth.application.DeviceAuthenticator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.english.common.security.Digests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class SyncLedgerTest {
    @Autowired MockMvc http;
    @Autowired JdbcTemplate db;
    @Autowired ObjectMapper json;
    @Autowired TransactionTemplate transactions;
    @Autowired SyncService service;
    @Autowired DeviceAuthenticator devices;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        String local = System.getenv("S07_TEST_DATABASE");
        if (local == null) {
            properties.add("spring.datasource.url", () -> "jdbc:tc:postgresql:16:///sync");
            properties.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        } else {
            // Flyway 创建独立 schema，每次运行都从空库开始。
            String schema = "s07_" + UUID.randomUUID().toString().replace("-", "");
            properties.add("spring.flyway.schemas", () -> schema);
            properties.add("spring.datasource.url", () -> local + "?currentSchema=" + schema);
            properties.add("spring.datasource.username", () -> System.getenv("S07_TEST_USER"));
            properties.add("spring.datasource.password", () -> System.getenv("S07_TEST_PASSWORD"));
        }
    }

    @Test
    void offlineEventsConvergeAcrossDevicesWithAtomicRewardsAndScopedPull() throws Exception {
        long family = db.queryForObject("insert into family_account default values returning id", Long.class);
        long child = child(family);
        long other = child(family);
        String first = device(family, child);
        String second = device(family, child);
        String epoch = read("/status", first, 200).at("/data/streamEpoch").asText();
        assertThat(epoch).isNotBlank();
        Instant time = LocalDate.now(ZoneId.of("Asia/Shanghai")).minusDays(1)
                .atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
        List<String> sentences = db.queryForList("select sentence_id from lesson_step_sentence where step_id='step-speak' order by sort_order", String.class);
        PushEvent speak = event(child, time, "step-speak", sentences.get(0));
        CompletableFuture<JsonNode> parallel = CompletableFuture.supplyAsync(() -> {
            try {
                return push(first, epoch, speak);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
        assertThat(List.of(status(push(first, epoch, speak)), status(parallel.get())))
                .containsExactlyInAnyOrder("ACCEPTED", "DUPLICATE");
        assertThat(status(push(second, epoch, event(child, time, "step-speak", sentences.get(0))))).isEqualTo("ACCEPTED");
        assertThat(count("star_ledger")).isEqualTo(1);
        JsonNode ack = push(first, epoch, speak).at("/data/results/0");
        assertThat(ack.path("grantOutcomes").get(0).path("status").asText()).isEqualTo("CONFIRMED");
        assertThat(ack.path("commitCursor").asLong()).isPositive();
        assertThat(push(first, epoch, speak).at("/data/results/0")).isEqualTo(ack);
        assertThat(db.queryForObject("select learning_day::text from star_ledger", String.class))
                .isEqualTo(time.atZone(ZoneId.of("Asia/Shanghai")).toLocalDate().toString());
        PushEvent changed = new PushEvent(speak.eventId(), child, time, "STEP_FINISHED", "step-listen", null);
        assertThat(push(first, epoch, changed).at("/data/results/0/code").asText()).isEqualTo("EVENT_ID_REUSED");
        JsonNode mixed = push(first, epoch, event(other,time,"step-listen",null),event(child,time,"step-listen",null));
        assertThat(status(mixed)).isEqualTo("REJECTED");
        assertThat(mixed.at("/data/results/1/status").asText()).isEqualTo("ACCEPTED");
        push(first, epoch, event(child,time,"step-recognize",null));
        assertThat(count("badge_award")).isZero();
        for (String sentence : sentences) {
            push(second, epoch, event(child,time,"step-speak",sentence));
        }
        push(first, epoch, event(child,time,"step-listen",null));
        assertThat(count("star_ledger")).isEqualTo(5);
        assertThat(count("daily_checkin")).isEqualTo(1);
        assertThat(count("badge_award")).isEqualTo(1);
        String path = "/pull?streamEpoch=" + epoch + "&childId=" + child + "&limit=1&cursor=";
        JsonNode page = read(path + "0", first, 200).get("data");
        assertThat(read(path + "0", first, 200).get("data")).isEqualTo(page);
        assertThat(page.get("hasMore").asBoolean()).isTrue();
        long cursor = 0;
        int seen = 0;
        do {
            page = read(path + cursor, first, 200).get("data");
            assertThat(page.get("changes").size()).isOne();
            long next = page.get("nextCursor").asLong();
            assertThat(next).isGreaterThan(cursor);
            cursor = next;
            seen++;
        } while (page.get("hasMore").asBoolean());
        assertThat(seen).isEqualTo(count("sync_change_log"));
        assertThat(read(path + cursor, first, 200).at("/data/changes").size()).isZero();
        read("/pull?streamEpoch=" + epoch + "&childId=" + other, first, 401);
        read("/pull?streamEpoch=stale&childId=" + child, first, 409);
        assertThat(push(first, epoch, event(child,time.minusSeconds(31L*86400),"step-listen",null))
                .at("/data/results/0/code").asText()).isEqualTo("EVENT_TOO_OLD");
        // 在事务最后的日志写入点制造真实 PostgreSQL 失败，之后可重试同一事件。
        PushEvent retry = event(child, Instant.now(), "step-speak", sentences.get(0));
        db.execute("alter table sync_change_log add constraint test_failure check (false) not valid");
        String body = body(epoch, retry);
        assertThat(http.perform(post("/api/v1/sync/push").header("Authorization", first)
                .contentType("application/json").content(body)).andReturn().getResponse().getStatus()).isEqualTo(500);
        assertThat(db.queryForObject("select count(*) from learning_event where event_id=?",
                Integer.class, UUID.fromString(retry.eventId()))).isZero();
        assertThat(count("star_ledger")).isEqualTo(5);
        db.execute("alter table sync_change_log drop constraint test_failure");
        assertThat(status(push(first, epoch, retry))).isEqualTo("ACCEPTED");
        var tooLarge = http.perform(post("/api/v1/sync/push").header("Authorization", first)
                .contentType("application/json").content(" ".repeat(256 * 1024 + 1))).andReturn().getResponse();
        assertThat(tooLarge.getStatus()).isEqualTo(413);
        verifyItemProgress(first, epoch, child, time.minusSeconds(86400));
        verifyCommitOrder(first, second, epoch, child);
        db.update("update client_device set revoked=true");
        read(path + "0", first, 401);
    }

    private void verifyItemProgress(String token, String epoch, long child, Instant time) throws Exception {
        int before = count("daily_step_progress");
        assertThat(status(push(token, epoch, event(child, time, "step-listen", "unrelated")))).isEqualTo("REJECTED");
        for (String animal : List.of("cat", "dog", "rabbit", "bird")) {
            push(token, epoch, event(child, time, "step-listen", "lexeme-" + animal));
            push(token, epoch, event(child, time, "step-recognize", "q-" + animal));
        }
        assertThat(count("daily_step_progress")).isEqualTo(before);
        push(token, epoch, event(child, time, "step-listen", "lexeme-fish"));
        push(token, epoch, event(child, time, "step-recognize", "q-fish"));
        assertThat(count("daily_step_progress")).isEqualTo(before + 2);
    }

    private void verifyCommitOrder(String first, String second, String epoch, long child) throws Exception {
        long start = db.queryForObject("select max(seq) from sync_change_log", Long.class);
        String path = "/pull?streamEpoch=" + epoch + "&childId=" + child + "&cursor=" + start;
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Instant today = Instant.now();
        var device = devices.requireActive(first);
        var writer = CompletableFuture.runAsync(() -> transactions.executeWithoutResult(tx -> {
            service.accept(device, epoch, event(child,today,"step-listen",null));
            inserted.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("commit barrier timed out");
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(error);
            }
        }));
        assertThat(inserted.await(10, TimeUnit.SECONDS)).isTrue();
        var later = CompletableFuture.supplyAsync(() -> service.accept(devices.requireActive(second),
                epoch, event(child,today,"step-recognize",null)));
        try {
            JsonNode waiting = read(path, first, 200).get("data");
            assertThat(waiting.get("changes").size()).isZero();
            assertThat(waiting.get("nextCursor").asLong()).isEqualTo(start);
        } finally {
            release.countDown();
        }
        writer.get(10, TimeUnit.SECONDS);
        assertThat(later.get(10, TimeUnit.SECONDS).status()).isEqualTo("ACCEPTED");
        assertThat(read(path, first, 200).at("/data/changes").size()).isEqualTo(2);
    }

    private int count(String table) {
        return db.queryForObject("select count(*) from " + table, Integer.class);
    }

    private long child(long family) {
        return db.queryForObject("insert into child_profile(family_id,nickname,age_band) values (?,'demo','AGE_5') returning id",
                Long.class, family);
    }

    private String device(long family, long child) {
        UUID id = UUID.randomUUID();
        String token = "dt." + UUID.randomUUID();
        db.update("insert into client_device(device_id,family_id,platform,token_hash) values (?,?,'H5',?)",
                id, family, Digests.sha256Hex(token));
        db.update("insert into device_child_binding values (?,?)", id, child);
        return "Bearer " + token;
    }

    private PushEvent event(long child, Instant time, String step, String target) {
        return new PushEvent(UUID.randomUUID().toString(), child, time, "STEP_FINISHED", step, target);
    }

    private String status(JsonNode envelope) {
        return envelope.at("/data/results/0/status").asText();
    }

    private String body(String epoch, PushEvent... events) throws Exception {
        return json.writeValueAsString(json.createObjectNode().put("schemaVersion", 2)
                .put("streamEpoch", epoch).set("events", json.valueToTree(events)));
    }

    private JsonNode push(String token, String epoch, PushEvent... events) throws Exception {
        var response = http.perform(post("/api/v1/sync/push").header("Authorization", token)
                .contentType("application/json").content(body(epoch, events))).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        return json.readTree(response.getContentAsByteArray());
    }

    private JsonNode read(String path, String token, int expected) throws Exception {
        var response = http.perform(get("/api/v1/sync" + path).header("Authorization", token))
                .andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(expected);
        return json.readTree(response.getContentAsByteArray());
    }
}
