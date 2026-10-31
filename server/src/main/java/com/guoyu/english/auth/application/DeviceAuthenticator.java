package com.guoyu.english.auth.application;

import org.springframework.stereotype.Service;

import com.guoyu.english.auth.domain.BearerTokens;
import com.guoyu.english.auth.domain.DevicePrincipal;
import com.guoyu.english.auth.infrastructure.SecretGenerator;
import com.guoyu.english.auth.infrastructure.persistence.ClientDeviceMapper;
import com.guoyu.english.auth.infrastructure.persistence.ClientDeviceRow;
import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;
import com.guoyu.english.common.security.Digests;

/**
 * 把设备令牌解析成仍有效的设备。找不到或已撤销都返回 DEVICE_REVOKED，不区分原因。
 */
@Service
public class DeviceAuthenticator {

    private final ClientDeviceMapper clientDeviceMapper;

    public DeviceAuthenticator(ClientDeviceMapper clientDeviceMapper) {
        this.clientDeviceMapper = clientDeviceMapper;
    }

    public DevicePrincipal requireActive(String authorization) {
        String token = BearerTokens.token(authorization);
        if (token == null || !token.startsWith(SecretGenerator.DEVICE_PREFIX)) {
            throw revoked();
        }
        ClientDeviceRow row = clientDeviceMapper.findByTokenHash(Digests.sha256Hex(token));
        if (row == null || row.isRevoked()) {
            throw revoked();
        }
        return new DevicePrincipal(row.getFamilyId(), row.getDeviceId());
    }

    private ApiException revoked() {
        return new ApiException(ApiCode.DEVICE_REVOKED, ApiCode.DEVICE_REVOKED.name());
    }
}
