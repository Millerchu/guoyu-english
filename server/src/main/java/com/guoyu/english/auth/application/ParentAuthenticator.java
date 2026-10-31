package com.guoyu.english.auth.application;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.guoyu.english.auth.domain.BearerTokens;
import com.guoyu.english.auth.domain.ParentPrincipal;
import com.guoyu.english.auth.domain.TimeSource;
import com.guoyu.english.auth.infrastructure.SecretGenerator;
import com.guoyu.english.auth.infrastructure.persistence.ParentSessionMapper;
import com.guoyu.english.auth.infrastructure.persistence.ParentSessionRow;
import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;
import com.guoyu.english.common.persistence.Timestamps;
import com.guoyu.english.common.security.Digests;

/**
 * 校验家长会话。设备被撤销后，绑在那台设备上的会话一并失效。
 */
@Service
public class ParentAuthenticator {

    private final ParentSessionMapper parentSessionMapper;

    private final TimeSource timeSource;

    public ParentAuthenticator(ParentSessionMapper parentSessionMapper, TimeSource timeSource) {
        this.parentSessionMapper = parentSessionMapper;
        this.timeSource = timeSource;
    }

    public ParentPrincipal require(String authorization) {
        String token = BearerTokens.token(authorization);
        if (token == null || !token.startsWith(SecretGenerator.PARENT_PREFIX)) {
            throw invalid();
        }
        ParentSessionRow row = parentSessionMapper.findByTokenHash(Digests.sha256Hex(token));
        if (row == null) {
            throw invalid();
        }
        Instant now = timeSource.now();
        Instant expiresAt = Timestamps.instant(row.getExpiresAt());
        if (expiresAt == null || !expiresAt.isAfter(now)) {
            throw invalid();
        }
        if (row.getRevokedAt() != null) {
            throw new ApiException(ApiCode.DEVICE_REVOKED, ApiCode.DEVICE_REVOKED.name());
        }
        return new ParentPrincipal(row.getFamilyId(), row.getId(), row.getDeviceId());
    }

    private ApiException invalid() {
        return new ApiException(ApiCode.INVALID_ARGUMENT, ApiCode.INVALID_ARGUMENT.name());
    }
}
