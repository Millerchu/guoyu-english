package com.guoyu.english.auth.application;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.guoyu.english.auth.api.PairCodeRequest;
import com.guoyu.english.auth.api.PairCodeResponse;
import com.guoyu.english.auth.api.PairDeviceRequest;
import com.guoyu.english.auth.api.PairDeviceResponse;
import com.guoyu.english.auth.domain.ParentPrincipal;
import com.guoyu.english.auth.domain.Platform;
import com.guoyu.english.auth.domain.TimeSource;
import com.guoyu.english.auth.infrastructure.AuthProperties;
import com.guoyu.english.auth.infrastructure.SecretGenerator;
import com.guoyu.english.auth.infrastructure.persistence.ClientDeviceMapper;
import com.guoyu.english.auth.infrastructure.persistence.ClientDeviceRow;
import com.guoyu.english.auth.infrastructure.persistence.DeviceChildBindingMapper;
import com.guoyu.english.auth.infrastructure.persistence.FamilyAccountMapper;
import com.guoyu.english.auth.infrastructure.persistence.PairCodeChildMapper;
import com.guoyu.english.auth.infrastructure.persistence.PairCodeMapper;
import com.guoyu.english.auth.infrastructure.persistence.PairCodeRow;
import com.guoyu.english.child.infrastructure.persistence.ChildProfileMapper;
import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;
import com.guoyu.english.common.persistence.Timestamps;
import com.guoyu.english.common.security.Digests;
import com.guoyu.english.common.web.RequestIds;

/**
 * 家长会话换 5 分钟配对码，配对码再换设备令牌。配对正文里出现 PIN 就拒绝，并且不消费配对码。
 */
@Service
public class PairingService {

    private static final Logger log = LoggerFactory.getLogger(PairingService.class);

    private static final int MAX_CHILD_IDS = 20;

    private static final int MAX_DEVICE_NAME_CODE_POINTS = 40;

    private final FamilyAccountMapper familyAccountMapper;

    private final ChildProfileMapper childProfileMapper;

    private final PairCodeMapper pairCodeMapper;

    private final PairCodeChildMapper pairCodeChildMapper;

    private final ClientDeviceMapper clientDeviceMapper;

    private final DeviceChildBindingMapper deviceChildBindingMapper;

    private final SecretGenerator secretGenerator;

    private final AuthProperties properties;

    private final TimeSource timeSource;

    public PairingService(
            FamilyAccountMapper familyAccountMapper,
            ChildProfileMapper childProfileMapper,
            PairCodeMapper pairCodeMapper,
            PairCodeChildMapper pairCodeChildMapper,
            ClientDeviceMapper clientDeviceMapper,
            DeviceChildBindingMapper deviceChildBindingMapper,
            SecretGenerator secretGenerator,
            AuthProperties properties,
            TimeSource timeSource) {
        this.familyAccountMapper = familyAccountMapper;
        this.childProfileMapper = childProfileMapper;
        this.pairCodeMapper = pairCodeMapper;
        this.pairCodeChildMapper = pairCodeChildMapper;
        this.clientDeviceMapper = clientDeviceMapper;
        this.deviceChildBindingMapper = deviceChildBindingMapper;
        this.secretGenerator = secretGenerator;
        this.properties = properties;
        this.timeSource = timeSource;
    }

    @Transactional(rollbackFor = Exception.class)
    public PairCodeResponse issue(ParentPrincipal parent, PairCodeRequest request) {
        if (request == null || request.hasUnknownFields()) {
            throw invalid();
        }
        String deviceName = normalizeDeviceName(request.getDeviceName());
        List<Long> childIds = normalizeChildIds(request.getChildIds());
        lockFamily(parent.getFamilyId());
        List<Long> existing = childProfileMapper.findExistingIds(parent.getFamilyId(), childIds);
        if (existing.size() != childIds.size()) {
            throw invalid();
        }
        Instant now = timeSource.now();
        String code = secretGenerator.pairCode();
        PairCodeRow row = new PairCodeRow();
        row.setFamilyId(parent.getFamilyId());
        row.setCodeHash(Digests.sha256Hex(code));
        row.setDeviceName(deviceName);
        row.setExpiresAt(Timestamps.offset(now.plus(properties.getPairCodeTtl())));
        row.setCreatedAt(Timestamps.offset(now));
        pairCodeMapper.insert(row);
        for (Long childId : childIds) {
            pairCodeChildMapper.insert(row.getId(), childId);
        }
        log.info("配对码已签发, requestId={}, familyId={}", RequestIds.current(), parent.getFamilyId());
        PairCodeResponse response = new PairCodeResponse();
        response.setPairCode(code);
        response.setExpiresAt(DateTimeFormatter.ISO_INSTANT.format(now.plus(properties.getPairCodeTtl())));
        response.setChildIds(childIds);
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public PairDeviceResponse pair(PairDeviceRequest request) {
        if (request == null || request.hasUnknownFields() || request.containsPin()) {
            throw invalid();
        }
        if (request.getPairCode() == null || request.getPairCode().isBlank()) {
            throw invalid();
        }
        UUID deviceId = parseDeviceId(request.getDeviceId());
        Platform platform = Platform.parse(request.getPlatform());
        ClientDeviceRow existing = clientDeviceMapper.findByIdForUpdate(deviceId);
        if (existing != null && !existing.isRevoked()) {
            throw invalid();
        }
        PairCodeRow code = pairCodeMapper.findByHashForUpdate(Digests.sha256Hex(request.getPairCode()));
        Instant now = timeSource.now();
        if (!usable(code, now)) {
            throw invalid();
        }
        if (existing != null && !existing.getFamilyId().equals(code.getFamilyId())) {
            throw invalid();
        }
        List<Long> childIds = pairCodeChildMapper.listChildIds(code.getId());
        if (childIds.isEmpty()) {
            throw invalid();
        }
        String token = secretGenerator.deviceToken();
        if (existing == null) {
            ClientDeviceRow created = new ClientDeviceRow();
            created.setDeviceId(deviceId);
            created.setFamilyId(code.getFamilyId());
            created.setPlatform(platform.name());
            created.setDisplayName(code.getDeviceName());
            created.setTokenHash(Digests.sha256Hex(token));
            created.setRevoked(false);
            created.setCreatedAt(Timestamps.offset(now));
            clientDeviceMapper.insert(created);
        } else {
            ClientDeviceRow rebound = new ClientDeviceRow();
            rebound.setDeviceId(deviceId);
            rebound.setPlatform(platform.name());
            rebound.setDisplayName(code.getDeviceName());
            rebound.setTokenHash(Digests.sha256Hex(token));
            clientDeviceMapper.rebind(rebound);
            deviceChildBindingMapper.deleteByDevice(deviceId);
        }
        for (Long childId : childIds) {
            deviceChildBindingMapper.insert(deviceId, childId);
        }
        int consumed = pairCodeMapper.markConsumed(code.getId(), Timestamps.offset(now));
        if (consumed != 1) {
            throw invalid();
        }
        log.info("设备已配对, requestId={}, familyId={}, deviceId={}",
                RequestIds.current(), code.getFamilyId(), deviceId);
        PairDeviceResponse response = new PairDeviceResponse();
        response.setDeviceId(deviceId.toString());
        response.setDeviceToken(token);
        response.setChildIds(childIds);
        return response;
    }

    private boolean usable(PairCodeRow code, Instant now) {
        if (code == null || code.getConsumedAt() != null) {
            return false;
        }
        Instant expiresAt = Timestamps.instant(code.getExpiresAt());
        return expiresAt != null && expiresAt.isAfter(now);
    }

    private void lockFamily(long familyId) {
        if (familyAccountMapper.lockById(familyId) == null) {
            throw new ApiException(ApiCode.INTERNAL_ERROR, ApiCode.INTERNAL_ERROR.name());
        }
    }

    private String normalizeDeviceName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String name = raw.trim();
        int length = name.codePointCount(0, name.length());
        if (length < 1 || length > MAX_DEVICE_NAME_CODE_POINTS) {
            throw invalid();
        }
        for (int i = 0; i < name.length(); i++) {
            if (Character.isISOControl(name.charAt(i))) {
                throw invalid();
            }
        }
        return name;
    }

    private List<Long> normalizeChildIds(List<Long> raw) {
        if (raw == null || raw.isEmpty() || raw.size() > MAX_CHILD_IDS) {
            throw invalid();
        }
        Set<Long> distinct = new LinkedHashSet<>();
        for (Long childId : raw) {
            if (childId == null || childId <= 0) {
                throw invalid();
            }
            distinct.add(childId);
        }
        if (distinct.isEmpty() || distinct.size() > MAX_CHILD_IDS) {
            throw invalid();
        }
        return new ArrayList<>(distinct);
    }

    private UUID parseDeviceId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw invalid();
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
    }

    private ApiException invalid() {
        return new ApiException(ApiCode.INVALID_ARGUMENT, ApiCode.INVALID_ARGUMENT.name());
    }
}
