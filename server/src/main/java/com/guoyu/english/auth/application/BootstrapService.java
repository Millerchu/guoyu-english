package com.guoyu.english.auth.application;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.guoyu.english.auth.api.BootstrapRequest;
import com.guoyu.english.auth.api.BootstrapResponse;
import com.guoyu.english.auth.api.ParentSessionResponse;
import com.guoyu.english.auth.domain.CanonicalPayloads;
import com.guoyu.english.auth.domain.IdempotencyKeys;
import com.guoyu.english.auth.domain.IdempotencyScope;
import com.guoyu.english.auth.domain.PinRules;
import com.guoyu.english.auth.domain.TimeSource;
import com.guoyu.english.auth.infrastructure.Argon2PinHasher;
import com.guoyu.english.auth.infrastructure.AuthProperties;
import com.guoyu.english.auth.infrastructure.persistence.AdvisoryLockMapper;
import com.guoyu.english.auth.infrastructure.persistence.FamilyAccountMapper;
import com.guoyu.english.auth.infrastructure.persistence.FamilyAccountRow;
import com.guoyu.english.auth.infrastructure.persistence.ParentCredentialMapper;
import com.guoyu.english.auth.infrastructure.persistence.ParentCredentialRow;
import com.guoyu.english.auth.infrastructure.persistence.RequestIdempotencyMapper;
import com.guoyu.english.auth.infrastructure.persistence.RequestIdempotencyRow;
import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;
import com.guoyu.english.common.persistence.Timestamps;
import com.guoyu.english.common.security.Digests;
import com.guoyu.english.common.web.RequestIds;

/**
 * 用环境变量里的一次性引导码创建唯一家庭。成功后不再接受另一把幂等键。
 */
@Service
public class BootstrapService {

    private static final Logger log = LoggerFactory.getLogger(BootstrapService.class);

    /**
     * 家庭还不存在时，用固定咨询锁串行化引导。
     */
    static final long BOOTSTRAP_LOCK_KEY = 6060603L;

    private final AuthProperties properties;

    private final AdvisoryLockMapper advisoryLockMapper;

    private final FamilyAccountMapper familyAccountMapper;

    private final ParentCredentialMapper parentCredentialMapper;

    private final RequestIdempotencyMapper requestIdempotencyMapper;

    private final ParentSessionService parentSessionService;

    private final Argon2PinHasher pinHasher;

    private final TimeSource timeSource;

    public BootstrapService(
            AuthProperties properties,
            AdvisoryLockMapper advisoryLockMapper,
            FamilyAccountMapper familyAccountMapper,
            ParentCredentialMapper parentCredentialMapper,
            RequestIdempotencyMapper requestIdempotencyMapper,
            ParentSessionService parentSessionService,
            Argon2PinHasher pinHasher,
            TimeSource timeSource) {
        this.properties = properties;
        this.advisoryLockMapper = advisoryLockMapper;
        this.familyAccountMapper = familyAccountMapper;
        this.parentCredentialMapper = parentCredentialMapper;
        this.requestIdempotencyMapper = requestIdempotencyMapper;
        this.parentSessionService = parentSessionService;
        this.pinHasher = pinHasher;
        this.timeSource = timeSource;
    }

    @Transactional(rollbackFor = Exception.class)
    public BootstrapResponse bootstrap(String idempotencyKey, BootstrapRequest request) {
        IdempotencyKeys.check(idempotencyKey);
        if (request == null || request.hasUnknownFields()) {
            throw invalid();
        }
        PinRules.check(request.getNewPin());
        String configured = properties.getBootstrapCode();
        if (configured == null || configured.isBlank()) {
            throw invalid();
        }
        if (!Digests.constantEquals(configured, request.getBootstrapCode())) {
            throw invalid();
        }
        advisoryLockMapper.lock(BOOTSTRAP_LOCK_KEY);
        FamilyAccountRow family = familyAccountMapper.lockSingleton();
        String fingerprint = CanonicalPayloads.join(idempotencyKey, request.getNewPin(), request.getBootstrapCode());
        if (family == null) {
            return createFamily(idempotencyKey, request.getNewPin(), fingerprint);
        }
        return retry(family, idempotencyKey, fingerprint);
    }

    private BootstrapResponse createFamily(String idempotencyKey, String pin, String fingerprint) {
        Instant now = timeSource.now();
        FamilyAccountRow family = new FamilyAccountRow();
        family.setAuthGeneration(1);
        family.setBootstrapConsumedAt(Timestamps.offset(now));
        family.setCreatedAt(Timestamps.offset(now));
        familyAccountMapper.insert(family);

        ParentCredentialRow credential = new ParentCredentialRow();
        credential.setFamilyId(family.getId());
        credential.setPinHash(pinHasher.hash(pin));
        credential.setUpdatedAt(Timestamps.offset(now));
        parentCredentialMapper.insert(credential);

        RequestIdempotencyRow record = new RequestIdempotencyRow();
        record.setScope(IdempotencyScope.BOOTSTRAP.name());
        record.setIdempotencyKey(idempotencyKey);
        record.setFamilyId(family.getId());
        record.setRequestHash(pinHasher.hash(fingerprint));
        record.setResourceId(family.getId());
        record.setCreatedAt(Timestamps.offset(now));
        requestIdempotencyMapper.insert(record);

        log.info("家庭引导完成, requestId={}, familyId={}", RequestIds.current(), family.getId());
        return toResponse(family.getId(), parentSessionService.issue(family.getId(), null));
    }

    private BootstrapResponse retry(FamilyAccountRow family, String idempotencyKey, String fingerprint) {
        RequestIdempotencyRow record = requestIdempotencyMapper.find(
                IdempotencyScope.BOOTSTRAP.name(), idempotencyKey);
        if (record == null || !family.getId().equals(record.getFamilyId())) {
            throw invalid();
        }
        if (!pinHasher.matches(fingerprint, record.getRequestHash())) {
            throw new ApiException(ApiCode.EVENT_ID_REUSED, ApiCode.EVENT_ID_REUSED.name());
        }
        Instant consumedAt = Timestamps.instant(family.getBootstrapConsumedAt());
        Instant now = timeSource.now();
        if (consumedAt == null || now.isAfter(consumedAt.plus(properties.getBootstrapRetryWindow()))) {
            throw invalid();
        }
        log.info("家庭引导幂等重试, requestId={}, familyId={}", RequestIds.current(), family.getId());
        return toResponse(family.getId(), parentSessionService.issue(family.getId(), null));
    }

    private BootstrapResponse toResponse(long familyId, ParentSessionResponse session) {
        BootstrapResponse response = new BootstrapResponse();
        response.setFamilyId(familyId);
        response.setParentToken(session.getParentToken());
        response.setExpiresAt(session.getExpiresAt());
        return response;
    }

    private ApiException invalid() {
        return new ApiException(ApiCode.INVALID_ARGUMENT, ApiCode.INVALID_ARGUMENT.name());
    }
}
