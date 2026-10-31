package com.guoyu.english.auth.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.guoyu.english.auth.api.DeviceSummaryResponse;
import com.guoyu.english.auth.api.RevokeDeviceResponse;
import com.guoyu.english.auth.domain.ParentPrincipal;
import com.guoyu.english.auth.domain.TimeSource;
import com.guoyu.english.auth.infrastructure.persistence.ClientDeviceMapper;
import com.guoyu.english.auth.infrastructure.persistence.ClientDeviceRow;
import com.guoyu.english.auth.infrastructure.persistence.DeviceChildBindingMapper;
import com.guoyu.english.auth.infrastructure.persistence.ParentSessionMapper;
import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;
import com.guoyu.english.common.persistence.Timestamps;
import com.guoyu.english.common.web.RequestIds;

/**
 * 家长查看和撤销设备。重复撤销保持已撤销，并让该设备上的家长会话失效。
 */
@Service
public class DeviceAdministrationService {

    private static final Logger log = LoggerFactory.getLogger(DeviceAdministrationService.class);

    private final ClientDeviceMapper clientDeviceMapper;

    private final DeviceChildBindingMapper deviceChildBindingMapper;

    private final ParentSessionMapper parentSessionMapper;

    private final TimeSource timeSource;

    public DeviceAdministrationService(
            ClientDeviceMapper clientDeviceMapper,
            DeviceChildBindingMapper deviceChildBindingMapper,
            ParentSessionMapper parentSessionMapper,
            TimeSource timeSource) {
        this.clientDeviceMapper = clientDeviceMapper;
        this.deviceChildBindingMapper = deviceChildBindingMapper;
        this.parentSessionMapper = parentSessionMapper;
        this.timeSource = timeSource;
    }

    @Transactional(readOnly = true)
    public List<DeviceSummaryResponse> list(ParentPrincipal parent) {
        List<DeviceSummaryResponse> responses = new ArrayList<>();
        for (ClientDeviceRow row : clientDeviceMapper.listByFamily(parent.getFamilyId())) {
            responses.add(toSummary(row));
        }
        return responses;
    }

    @Transactional(rollbackFor = Exception.class)
    public RevokeDeviceResponse revoke(ParentPrincipal parent, UUID deviceId) {
        ClientDeviceRow row = clientDeviceMapper.findByIdForUpdate(deviceId);
        if (row == null || row.getFamilyId() == null || row.getFamilyId() != parent.getFamilyId()) {
            throw new ApiException(ApiCode.INVALID_ARGUMENT, ApiCode.INVALID_ARGUMENT.name());
        }
        if (!row.isRevoked()) {
            Instant now = timeSource.now();
            clientDeviceMapper.markRevoked(deviceId, Timestamps.offset(now));
            parentSessionMapper.revokeByDevice(deviceId, Timestamps.offset(now));
            log.info("设备已撤销, requestId={}, familyId={}, deviceId={}",
                    RequestIds.current(), parent.getFamilyId(), deviceId);
        }
        RevokeDeviceResponse response = new RevokeDeviceResponse();
        response.setDeviceId(deviceId.toString());
        response.setRevoked(true);
        return response;
    }

    private DeviceSummaryResponse toSummary(ClientDeviceRow row) {
        DeviceSummaryResponse response = new DeviceSummaryResponse();
        response.setDeviceId(row.getDeviceId().toString());
        response.setPlatform(row.getPlatform());
        response.setDeviceName(row.getDisplayName());
        response.setRevoked(row.isRevoked());
        response.setChildIds(deviceChildBindingMapper.listChildIds(row.getDeviceId()));
        return response;
    }
}
