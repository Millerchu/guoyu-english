package com.guoyu.english.sync;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.english.auth.domain.DevicePrincipal;
import com.guoyu.english.auth.domain.TimeSource;
import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;
import com.guoyu.english.common.security.Digests;
import com.guoyu.english.reward.DailyRewards;

@Service
public class SyncService {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final Set<String> STEPS = Set.of("step-listen", "step-speak", "step-recognize");
    private final SyncMapper mapper;
    private final DailyRewards rewards;
    private final TimeSource clock;
    private final ObjectMapper json;

    public SyncService(SyncMapper mapper, DailyRewards rewards, TimeSource clock, ObjectMapper json) {
        this.mapper = mapper;
        this.rewards = rewards;
        this.clock = clock;
        this.json = json;
    }

    public SyncStatus status(DevicePrincipal device) {
        return new SyncStatus(mapper.epoch(device.getFamilyId()));
    }

    public void requireEpoch(String expected, String supplied) {
        if (expected == null || !expected.equals(supplied)) {
            throw new ApiException(ApiCode.STREAM_EPOCH_CHANGED, "STREAM_EPOCH_CHANGED");
        }
    }

    private void authorize(DevicePrincipal device, long child) {
        if (mapper.boundDevice(device.getDeviceId(), device.getFamilyId(), child) == null) {
            throw new ApiException(ApiCode.DEVICE_REVOKED, "DEVICE_REVOKED");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public EventAck accept(DevicePrincipal device, String epoch, PushEvent event) {
        String rawId = event.eventId();
        UUID id;
        try {
            id = UUID.fromString(rawId);
            if (!id.toString().equalsIgnoreCase(rawId)) {
                return reject(rawId, "INVALID_ARGUMENT");
            }
        } catch (IllegalArgumentException ex) {
            return reject(rawId, "INVALID_ARGUMENT");
        }
        if (event.childId() == null || event.occurredAt() == null || event.stepId() == null
                || !"STEP_FINISHED".equals(event.eventType()) || !STEPS.contains(event.stepId())
                || (event.targetId() != null && !event.targetId().matches("[A-Za-z0-9._:-]{1,80}"))) {
            return reject(rawId, "INVALID_ARGUMENT");
        }
        long child = event.childId();
        requireEpoch(mapper.lockStream(device.getFamilyId()), epoch);
        if (mapper.boundDevice(device.getDeviceId(), device.getFamilyId(), child) == null) {
            if (mapper.activeDevice(device.getDeviceId()) == null) {
                throw new ApiException(ApiCode.DEVICE_REVOKED, "DEVICE_REVOKED");
            }
            return reject(rawId, "INVALID_ARGUMENT");
        }
        String hash = Digests.sha256Hex(device.getDeviceId() + "|" + child + "|" + event.occurredAt()
                + "|" + event.stepId() + "|" + (event.targetId() == null ? "" : event.targetId()));
        String existing = mapper.hash(id);
        if (existing != null) {
            return duplicate(event, epoch, id, hash, existing);
        }
        Instant now = clock.now();
        Instant occurred = event.occurredAt();
        if (occurred.isBefore(now.minus(Duration.ofDays(30)))) {
            return reject(rawId, "EVENT_TOO_OLD");
        }
        if (occurred.isAfter(now.plus(Duration.ofMinutes(5)))) {
            return reject(rawId, "TIME_REVIEW_REQUIRED");
        }
        if ("step-speak".equals(event.stepId()) && !mapper.speakTarget(event.targetId())) {
            return reject(rawId, "INVALID_ARGUMENT");
        }
        if (!"step-speak".equals(event.stepId()) && event.targetId() != null
                && !mapper.activityTarget(event.stepId(), event.targetId())) {
            return reject(rawId, "INVALID_ARGUMENT");
        }
        LocalDate day = occurred.atZone(SHANGHAI).toLocalDate();
        if (mapper.insertEvent(id, child, device.getDeviceId(), day, occurred.atOffset(ZoneOffset.UTC),
                event.stepId(), event.targetId(), hash) == 0) {
            return duplicate(event, epoch, id, hash, mapper.hash(id));
        }
        rewards.apply(child, day, id, event.stepId(), event.targetId(), epoch, now.atZone(SHANGHAI).toLocalDate().minusDays(30));
        long cursor = mapper.lastSeq(child);
        mapper.settle(id, cursor);
        return confirmed(event, epoch, cursor, "ACCEPTED");
    }

    @Transactional(rollbackFor = Exception.class)
    public PullResult pull(DevicePrincipal device, long child, String epoch, long cursor, int limit) {
        authorize(device, child);
        requireEpoch(mapper.epoch(device.getFamilyId()), epoch);
        if (cursor < 0 || limit < 1 || limit > 500) {
            throw new ApiException(ApiCode.INVALID_ARGUMENT, "INVALID_ARGUMENT");
        }
        if (cursor > mapper.lastSeq(child)) {
            throw new ApiException(ApiCode.EVENT_CONFLICT, "EVENT_CONFLICT");
        }
        // 奖励快照包含近 30 天的键集合，每页限定条数以适配客户端 1 MiB 响应上限。
        int pageSize = Math.min(limit, 20);
        List<String> rows = mapper.page(child, cursor, pageSize + 1);
        List<PullChange> changes = new ArrayList<>();
        try {
            for (int i = 0; i < Math.min(pageSize, rows.size()); i++) {
                changes.add(json.readValue(rows.get(i), PullChange.class));
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid stored sync change", ex);
        }
        long next = changes.isEmpty() ? cursor : changes.get(changes.size() - 1).seq();
        return new PullResult(epoch, changes, Long.toString(next), rows.size() > pageSize);
    }

    private EventAck duplicate(PushEvent event, String epoch, UUID id, String hash, String previous) {
        return hash.equals(previous) ? confirmed(event, epoch, mapper.settledCursor(id), "DUPLICATE")
                : reject(event.eventId(), "EVENT_ID_REUSED");
    }

    private static EventAck confirmed(PushEvent event, String epoch, long cursor, String status) {
        List<GrantOutcome> outcomes = List.of();
        if ("step-speak".equals(event.stepId())) {
            String key = event.childId() + ":" + event.occurredAt().atZone(SHANGHAI).toLocalDate()
                    + ":SPEAK_STAR:" + event.targetId();
            outcomes = List.of(new GrantOutcome(key, "CONFIRMED", 1));
        }
        return new EventAck(event.eventId(), status, null, epoch, Long.toString(cursor), outcomes);
    }

    static EventAck reject(String id, String reason) {
        return new EventAck(id, "REJECTED", reason, null, null, null);
    }
}
