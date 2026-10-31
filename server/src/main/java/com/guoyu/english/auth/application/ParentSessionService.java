package com.guoyu.english.auth.application;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.guoyu.english.auth.api.ParentSessionResponse;
import com.guoyu.english.auth.domain.DevicePrincipal;
import com.guoyu.english.auth.domain.PinDelayPolicy;
import com.guoyu.english.auth.domain.TimeSource;
import com.guoyu.english.auth.infrastructure.Argon2PinHasher;
import com.guoyu.english.auth.infrastructure.AuthProperties;
import com.guoyu.english.auth.infrastructure.SecretGenerator;
import com.guoyu.english.auth.infrastructure.persistence.ClientDeviceMapper;
import com.guoyu.english.auth.infrastructure.persistence.ClientDeviceRow;
import com.guoyu.english.auth.infrastructure.persistence.ParentCredentialMapper;
import com.guoyu.english.auth.infrastructure.persistence.ParentCredentialRow;
import com.guoyu.english.auth.infrastructure.persistence.ParentSessionMapper;
import com.guoyu.english.auth.infrastructure.persistence.ParentSessionRow;
import com.guoyu.english.auth.infrastructure.persistence.PinAttemptMapper;
import com.guoyu.english.auth.infrastructure.persistence.PinAttemptRow;
import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.persistence.Timestamps;
import com.guoyu.english.common.security.Digests;
import com.guoyu.english.common.web.RequestIds;

/**
 * 签发 15 分钟家长会话。PIN 失败次数和延迟在同一事务里写入数据库，再返回失败。
 */
@Service
public class ParentSessionService {

    private static final Logger log = LoggerFactory.getLogger(ParentSessionService.class);

    private final ClientDeviceMapper clientDeviceMapper;

    private final ParentCredentialMapper parentCredentialMapper;

    private final ParentSessionMapper parentSessionMapper;

    private final PinAttemptMapper pinAttemptMapper;

    private final Argon2PinHasher pinHasher;

    private final SecretGenerator secretGenerator;

    private final AuthProperties properties;

    private final TimeSource timeSource;

    public ParentSessionService(
            ClientDeviceMapper clientDeviceMapper,
            ParentCredentialMapper parentCredentialMapper,
            ParentSessionMapper parentSessionMapper,
            PinAttemptMapper pinAttemptMapper,
            Argon2PinHasher pinHasher,
            SecretGenerator secretGenerator,
            AuthProperties properties,
            TimeSource timeSource) {
        this.clientDeviceMapper = clientDeviceMapper;
        this.parentCredentialMapper = parentCredentialMapper;
        this.parentSessionMapper = parentSessionMapper;
        this.pinAttemptMapper = pinAttemptMapper;
        this.pinHasher = pinHasher;
        this.secretGenerator = secretGenerator;
        this.properties = properties;
        this.timeSource = timeSource;
    }

    @Transactional(rollbackFor = Exception.class)
    public ParentSessionResponse issue(long familyId, UUID deviceId) {
        return insertSession(familyId, deviceId, timeSource.now());
    }

    @Transactional(rollbackFor = Exception.class)
    public ParentSessionOutcome authenticate(DevicePrincipal device, String pin) {
        ClientDeviceRow locked = clientDeviceMapper.findByIdForUpdate(device.getDeviceId());
        if (locked == null || locked.isRevoked() || locked.getFamilyId() != device.getFamilyId()) {
            return ParentSessionOutcome.failure(ApiCode.DEVICE_REVOKED);
        }
        Instant now = timeSource.now();
        PinAttemptRow attempt = pinAttemptMapper.findForUpdate(device.getFamilyId(), device.getDeviceId());
        if (isLocked(attempt, now)) {
            log.warn("家长会话仍在延迟中, requestId={}, deviceId={}", RequestIds.current(), device.getDeviceId());
            return ParentSessionOutcome.failure(ApiCode.INVALID_ARGUMENT);
        }
        ParentCredentialRow credential = parentCredentialMapper.findByFamilyId(device.getFamilyId());
        if (credential == null || !pinHasher.matches(pin, credential.getPinHash())) {
            recordFailure(device, attempt, now);
            log.warn("家长会话未通过, requestId={}, deviceId={}", RequestIds.current(), device.getDeviceId());
            return ParentSessionOutcome.failure(ApiCode.INVALID_ARGUMENT);
        }
        clearFailures(device, now);
        log.info("家长会话已建立, requestId={}, deviceId={}", RequestIds.current(), device.getDeviceId());
        return ParentSessionOutcome.success(insertSession(device.getFamilyId(), device.getDeviceId(), now));
    }

    private boolean isLocked(PinAttemptRow attempt, Instant now) {
        if (attempt == null || attempt.getLockedUntil() == null) {
            return false;
        }
        Instant lockedUntil = Timestamps.instant(attempt.getLockedUntil());
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    private void recordFailure(DevicePrincipal device, PinAttemptRow attempt, Instant now) {
        int nextCount = (attempt == null || attempt.getFailedCount() == null ? 0 : attempt.getFailedCount()) + 1;
        Duration delay = PinDelayPolicy.delayAfterFailure(nextCount);
        PinAttemptRow row = new PinAttemptRow();
        row.setFamilyId(device.getFamilyId());
        row.setDeviceId(device.getDeviceId());
        row.setFailedCount(nextCount);
        row.setLockedUntil(delay.isZero() ? null : Timestamps.offset(now.plus(delay)));
        row.setUpdatedAt(Timestamps.offset(now));
        pinAttemptMapper.upsert(row);
    }

    private void clearFailures(DevicePrincipal device, Instant now) {
        PinAttemptRow row = new PinAttemptRow();
        row.setFamilyId(device.getFamilyId());
        row.setDeviceId(device.getDeviceId());
        row.setFailedCount(0);
        row.setLockedUntil(null);
        row.setUpdatedAt(Timestamps.offset(now));
        pinAttemptMapper.upsert(row);
    }

    private ParentSessionResponse insertSession(long familyId, UUID deviceId, Instant now) {
        String token = secretGenerator.parentToken();
        Instant expiresAt = now.plus(properties.getParentSessionTtl());
        ParentSessionRow row = new ParentSessionRow();
        row.setFamilyId(familyId);
        row.setDeviceId(deviceId);
        row.setTokenHash(Digests.sha256Hex(token));
        row.setExpiresAt(Timestamps.offset(expiresAt));
        row.setCreatedAt(Timestamps.offset(now));
        parentSessionMapper.insert(row);
        ParentSessionResponse response = new ParentSessionResponse();
        response.setParentToken(token);
        response.setExpiresAt(DateTimeFormatter.ISO_INSTANT.format(expiresAt));
        return response;
    }
}
