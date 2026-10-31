package com.guoyu.english.auth.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.guoyu.english.auth.api.BootstrapRequest;
import com.guoyu.english.auth.api.BootstrapResponse;
import com.guoyu.english.auth.application.BootstrapService;
import com.guoyu.english.common.api.ApiResponse;
import com.guoyu.english.common.api.ApiResponses;

/**
 * 空库引导。不要求已有令牌。
 */
@RestController
@RequestMapping("/api/v1/parent")
public class ParentBootstrapController {

    private final BootstrapService bootstrapService;

    public ParentBootstrapController(BootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @PostMapping(path = "/bootstrap", consumes = "application/json", produces = "application/json;charset=UTF-8")
    public ApiResponse<BootstrapResponse> bootstrap(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody BootstrapRequest request) {
        return ApiResponses.ok(bootstrapService.bootstrap(idempotencyKey, request));
    }
}
