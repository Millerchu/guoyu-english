package com.guoyu.english.child.application;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.guoyu.english.auth.domain.IdempotencyKeys;
import com.guoyu.english.auth.domain.IdempotencyScope;
import com.guoyu.english.auth.domain.ParentPrincipal;
import com.guoyu.english.auth.domain.TimeSource;
import com.guoyu.english.auth.infrastructure.persistence.FamilyAccountMapper;
import com.guoyu.english.auth.infrastructure.persistence.RequestIdempotencyMapper;
import com.guoyu.english.auth.infrastructure.persistence.RequestIdempotencyRow;
import com.guoyu.english.child.api.ChildProfileResponse;
import com.guoyu.english.child.api.CreateChildRequest;
import com.guoyu.english.child.domain.ChildDraft;
import com.guoyu.english.child.domain.ChildProfileRules;
import com.guoyu.english.child.infrastructure.persistence.ChildProfileMapper;
import com.guoyu.english.child.infrastructure.persistence.ChildProfileRow;
import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;
import com.guoyu.english.common.persistence.Timestamps;
import com.guoyu.english.common.security.Digests;
import com.guoyu.english.common.web.RequestIds;

/**
 * 创建和列出儿童档案。相同幂等键和相同载荷返回原来的儿童，换载荷不建第二份。
 */
@Service
public class ChildProfileService {

    private static final Logger log = LoggerFactory.getLogger(ChildProfileService.class);

    private final FamilyAccountMapper familyAccountMapper;

    private final ChildProfileMapper childProfileMapper;

    private final RequestIdempotencyMapper requestIdempotencyMapper;

    private final TimeSource timeSource;

    public ChildProfileService(
            FamilyAccountMapper familyAccountMapper,
            ChildProfileMapper childProfileMapper,
            RequestIdempotencyMapper requestIdempotencyMapper,
            TimeSource timeSource) {
        this.familyAccountMapper = familyAccountMapper;
        this.childProfileMapper = childProfileMapper;
        this.requestIdempotencyMapper = requestIdempotencyMapper;
        this.timeSource = timeSource;
    }

    @Transactional(readOnly = true)
    public List<ChildProfileResponse> list(ParentPrincipal parent) {
        List<ChildProfileResponse> responses = new ArrayList<>();
        for (ChildProfileRow row : childProfileMapper.listByFamily(parent.getFamilyId())) {
            responses.add(toResponse(row));
        }
        return responses;
    }

    @Transactional(rollbackFor = Exception.class)
    public ChildProfileResponse create(ParentPrincipal parent, String idempotencyKey, CreateChildRequest request) {
        IdempotencyKeys.check(idempotencyKey);
        ChildDraft draft = ChildProfileRules.normalize(request);
        if (familyAccountMapper.lockById(parent.getFamilyId()) == null) {
            throw new ApiException(ApiCode.INTERNAL_ERROR, ApiCode.INTERNAL_ERROR.name());
        }
        String hash = Digests.sha256Hex(ChildProfileRules.fingerprint(draft));
        RequestIdempotencyRow existing = requestIdempotencyMapper.find(
                IdempotencyScope.CHILD_CREATE.name(), idempotencyKey);
        if (existing != null) {
            return replay(parent, existing, hash);
        }
        Instant now = timeSource.now();
        ChildProfileRow row = new ChildProfileRow();
        row.setFamilyId(parent.getFamilyId());
        row.setNickname(draft.getNickname());
        row.setAgeBand(draft.getAgeBand().name());
        row.setDailyMinutes(draft.getDailyMinutes());
        row.setTimezoneId(draft.getTimezoneId());
        row.setTimezoneVersion(1);
        row.setProfileVersion(1);
        row.setCreatedAt(Timestamps.offset(now));
        childProfileMapper.insert(row);

        RequestIdempotencyRow record = new RequestIdempotencyRow();
        record.setScope(IdempotencyScope.CHILD_CREATE.name());
        record.setIdempotencyKey(idempotencyKey);
        record.setFamilyId(parent.getFamilyId());
        record.setRequestHash(hash);
        record.setResourceId(row.getId());
        record.setCreatedAt(Timestamps.offset(now));
        requestIdempotencyMapper.insert(record);
        log.info("儿童档案已创建, requestId={}, familyId={}, childId={}",
                RequestIds.current(), parent.getFamilyId(), row.getId());
        return toResponse(row);
    }

    private ChildProfileResponse replay(ParentPrincipal parent, RequestIdempotencyRow existing, String hash) {
        if (!Long.valueOf(parent.getFamilyId()).equals(existing.getFamilyId())
                || existing.getRequestHash() == null
                || !existing.getRequestHash().equals(hash)) {
            throw new ApiException(ApiCode.EVENT_ID_REUSED, ApiCode.EVENT_ID_REUSED.name());
        }
        ChildProfileRow row = childProfileMapper.findById(existing.getResourceId());
        if (row == null || row.getFamilyId() == null || row.getFamilyId() != parent.getFamilyId()) {
            throw new ApiException(ApiCode.EVENT_ID_REUSED, ApiCode.EVENT_ID_REUSED.name());
        }
        log.info("儿童档案幂等返回, requestId={}, familyId={}, childId={}",
                RequestIds.current(), parent.getFamilyId(), row.getId());
        return toResponse(row);
    }

    private ChildProfileResponse toResponse(ChildProfileRow row) {
        ChildProfileResponse response = new ChildProfileResponse();
        response.setChildId(row.getId());
        response.setNickname(row.getNickname());
        response.setAgeBand(row.getAgeBand());
        response.setDailyMinutes(row.getDailyMinutes());
        response.setTimezoneId(row.getTimezoneId());
        response.setTimezoneVersion(row.getTimezoneVersion());
        response.setProfileVersion(row.getProfileVersion());
        response.setPendingTimezone(row.getPendingTimezone());
        if (row.getPendingTimezoneEffectiveAt() != null) {
            response.setPendingTimezoneEffectiveAt(
                    DateTimeFormatter.ISO_INSTANT.format(Timestamps.instant(row.getPendingTimezoneEffectiveAt())));
        }
        if (row.getIssuedThrough() != null) {
            response.setIssuedThrough(row.getIssuedThrough().toString());
        }
        return response;
    }
}
