package com.guoyu.english.auth.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.guoyu.english.auth.api.PairCodeRequest;
import com.guoyu.english.auth.api.PairCodeResponse;
import com.guoyu.english.auth.application.PairingService;
import com.guoyu.english.auth.application.ParentAuthenticator;
import com.guoyu.english.common.api.ApiResponse;
import com.guoyu.english.common.api.ApiResponses;

/**
 * 家长会话换 5 分钟一次性配对码。
 */
@RestController
@RequestMapping("/api/v1/parent/pair-codes")
public class PairCodeController {

    private final ParentAuthenticator parentAuthenticator;

    private final PairingService pairingService;

    public PairCodeController(ParentAuthenticator parentAuthenticator, PairingService pairingService) {
        this.parentAuthenticator = parentAuthenticator;
        this.pairingService = pairingService;
    }

    @PostMapping(consumes = "application/json", produces = "application/json;charset=UTF-8")
    public ApiResponse<PairCodeResponse> issue(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody PairCodeRequest request) {
        return ApiResponses.ok(pairingService.issue(parentAuthenticator.require(authorization), request));
    }
}
