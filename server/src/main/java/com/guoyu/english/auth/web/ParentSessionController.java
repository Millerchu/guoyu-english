package com.guoyu.english.auth.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.guoyu.english.auth.api.ParentSessionRequest;
import com.guoyu.english.auth.api.ParentSessionResponse;
import com.guoyu.english.auth.application.DeviceAuthenticator;
import com.guoyu.english.auth.application.ParentSessionService;
import com.guoyu.english.auth.domain.DevicePrincipal;
import com.guoyu.english.auth.domain.PinRules;
import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;
import com.guoyu.english.common.api.ApiResponse;
import com.guoyu.english.common.api.ApiResponses;

/**
 * 已绑定设备用令牌和 PIN 换家长会话。这里不签发设备令牌。
 */
@RestController
@RequestMapping("/api/v1/parent")
public class ParentSessionController {

    private final DeviceAuthenticator deviceAuthenticator;

    private final ParentSessionService parentSessionService;

    public ParentSessionController(
            DeviceAuthenticator deviceAuthenticator,
            ParentSessionService parentSessionService) {
        this.deviceAuthenticator = deviceAuthenticator;
        this.parentSessionService = parentSessionService;
    }

    @PostMapping(path = "/sessions", consumes = "application/json", produces = "application/json;charset=UTF-8")
    public ApiResponse<ParentSessionResponse> open(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ParentSessionRequest request) {
        if (request == null || request.hasUnknownFields()) {
            throw new ApiException(ApiCode.INVALID_ARGUMENT, ApiCode.INVALID_ARGUMENT.name());
        }
        PinRules.check(request.getPin());
        DevicePrincipal device = deviceAuthenticator.requireActive(authorization);
        ParentSessionResponse response = parentSessionService.authenticate(device, request.getPin()).required();
        return ApiResponses.ok(response);
    }
}
