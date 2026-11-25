package com.guoyu.english.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.english.auth.application.DeviceAuthenticator;
import com.guoyu.english.auth.domain.DevicePrincipal;
import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;
import com.guoyu.english.common.api.ApiResponse;
import com.guoyu.english.common.api.ApiResponses;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {
    private static final int MAX_BYTES = 256 * 1024;
    private static final int MAX_EVENTS = 100;
    private final DeviceAuthenticator auth;
    private final SyncService service;
    private final ObjectMapper json;

    public SyncController(DeviceAuthenticator auth, SyncService service, ObjectMapper json) {
        this.auth = auth;
        this.service = service;
        this.json = json;
    }

    @GetMapping("/status")
    public ApiResponse<SyncStatus> status(@RequestHeader(value="Authorization", required=false) String token) {
        return ApiResponses.ok(service.status(auth.requireActive(token)));
    }

    @PostMapping(value="/push", consumes="application/json")
    public ResponseEntity<?> push(HttpServletRequest request) throws IOException {
        DevicePrincipal device = auth.requireActive(request.getHeader("Authorization"));
        byte[] body = request.getInputStream().readNBytes(MAX_BYTES + 1);
        if (body.length > MAX_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(ApiResponses.of(ApiCode.INVALID_ARGUMENT, "Push exceeds 256 KiB", null));
        }
        PushBatch batch;
        try {
            batch = json.readerFor(PushBatch.class).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readValue(body);
        } catch (JsonProcessingException ex) {
            throw invalid();
        }
        if (batch == null || !Integer.valueOf(2).equals(batch.schemaVersion()) || batch.events() == null
                || batch.events().size() > MAX_EVENTS) {
            throw invalid();
        }
        service.requireEpoch(service.status(device).streamEpoch(), batch.streamEpoch());
        // 没有可关联的 ID 时整批拒绝，避免客户端收到无法对应到 outbox 的 ACK。
        for (JsonNode node : batch.events()) {
            if (node == null || !node.path("eventId").isTextual() || node.path("eventId").asText().isBlank()) {
                throw invalid();
            }
        }
        List<EventAck> results = new ArrayList<>();
        for (JsonNode node : batch.events()) {
            try {
                if (!node.path("childId").isIntegralNumber() || !node.path("childId").canConvertToLong()
                        || !node.path("occurredAt").isTextual() || !node.path("occurredAt").asText().endsWith("Z")) {
                    results.add(SyncService.reject(node.path("eventId").asText(), "INVALID_ARGUMENT"));
                    continue;
                }
                PushEvent event = json.readerFor(PushEvent.class)
                        .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(node);
                results.add(service.accept(device, batch.streamEpoch(), event));
            } catch (JsonProcessingException ex) {
                results.add(SyncService.reject(node.get("eventId").asText(), "INVALID_ARGUMENT"));
            }
        }
        return ResponseEntity.ok(ApiResponses.ok(new PushResult(batch.streamEpoch(), results)));
    }

    @GetMapping("/pull")
    public ApiResponse<PullResult> pull(@RequestHeader(value="Authorization", required=false) String token,
            @RequestParam long childId, @RequestParam String streamEpoch,
            @RequestParam(defaultValue="0") long cursor, @RequestParam(defaultValue="100") int limit) {
        return ApiResponses.ok(service.pull(auth.requireActive(token), childId, streamEpoch, cursor, limit));
    }

    private static ApiException invalid() {
        return new ApiException(ApiCode.INVALID_ARGUMENT, "INVALID_ARGUMENT");
    }
}
