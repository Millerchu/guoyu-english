package com.guoyu.english.auth.web;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.guoyu.english.auth.api.DeviceSummaryResponse;
import com.guoyu.english.auth.api.RevokeDeviceResponse;
import com.guoyu.english.auth.application.DeviceAdministrationService;
import com.guoyu.english.auth.application.ParentAuthenticator;
import com.guoyu.english.common.api.ApiResponse;
import com.guoyu.english.common.api.ApiResponses;

/**
 * 家长查看设备并撤销。撤销后该设备令牌不能再访问受保护接口。
 */
@RestController
@RequestMapping("/api/v1/parent/devices")
public class ParentDeviceController {

    private final ParentAuthenticator parentAuthenticator;

    private final DeviceAdministrationService deviceAdministrationService;

    public ParentDeviceController(
            ParentAuthenticator parentAuthenticator,
            DeviceAdministrationService deviceAdministrationService) {
        this.parentAuthenticator = parentAuthenticator;
        this.deviceAdministrationService = deviceAdministrationService;
    }

    @GetMapping(produces = "application/json;charset=UTF-8")
    public ApiResponse<List<DeviceSummaryResponse>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponses.ok(deviceAdministrationService.list(parentAuthenticator.require(authorization)));
    }

    @DeleteMapping(path = "/{deviceId}", produces = "application/json;charset=UTF-8")
    public ApiResponse<RevokeDeviceResponse> revoke(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID deviceId) {
        return ApiResponses.ok(deviceAdministrationService.revoke(
                parentAuthenticator.require(authorization), deviceId));
    }
}
