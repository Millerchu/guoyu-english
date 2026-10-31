package com.guoyu.english.auth.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.guoyu.english.auth.api.PairDeviceRequest;
import com.guoyu.english.auth.api.PairDeviceResponse;
import com.guoyu.english.auth.application.PairingService;
import com.guoyu.english.common.api.ApiResponse;
import com.guoyu.english.common.api.ApiResponses;

/**
 * 用配对码换设备令牌。请求体不接受 PIN。
 */
@RestController
@RequestMapping("/api/v1/devices")
public class DevicePairController {

    private final PairingService pairingService;

    public DevicePairController(PairingService pairingService) {
        this.pairingService = pairingService;
    }

    @PostMapping(path = "/pair", consumes = "application/json", produces = "application/json;charset=UTF-8")
    public ApiResponse<PairDeviceResponse> pair(@RequestBody PairDeviceRequest request) {
        return ApiResponses.ok(pairingService.pair(request));
    }
}
