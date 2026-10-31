package com.guoyu.english.child.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.guoyu.english.auth.application.ParentAuthenticator;
import com.guoyu.english.child.api.ChildProfileResponse;
import com.guoyu.english.child.api.CreateChildRequest;
import com.guoyu.english.child.application.ChildProfileService;
import com.guoyu.english.common.api.ApiResponse;
import com.guoyu.english.common.api.ApiResponses;

/**
 * 家长会话下的儿童建档和列表。相同幂等键不重复建档。
 */
@RestController
@RequestMapping("/api/v1/parent/children")
public class ChildController {

    private final ParentAuthenticator parentAuthenticator;

    private final ChildProfileService childProfileService;

    public ChildController(ParentAuthenticator parentAuthenticator, ChildProfileService childProfileService) {
        this.parentAuthenticator = parentAuthenticator;
        this.childProfileService = childProfileService;
    }

    @GetMapping(produces = "application/json;charset=UTF-8")
    public ApiResponse<List<ChildProfileResponse>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponses.ok(childProfileService.list(parentAuthenticator.require(authorization)));
    }

    @PostMapping(consumes = "application/json", produces = "application/json;charset=UTF-8")
    public ApiResponse<ChildProfileResponse> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreateChildRequest request) {
        return ApiResponses.ok(childProfileService.create(
                parentAuthenticator.require(authorization), idempotencyKey, request));
    }
}
