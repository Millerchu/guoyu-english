package com.guoyu.english.common.health;

import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.guoyu.english.common.api.ApiResponse;
import com.guoyu.english.common.api.ApiResponses;

/**
 * 供局域网探测进程是否起来。不校验设备令牌。
 */
@RestController
@RequestMapping("/api/v1/system")
public class HealthController {

    private final BuildProperties buildProperties;

    public HealthController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping(path = "/health", produces = "application/json;charset=UTF-8")
    public ApiResponse<HealthData> health() {
        HealthData data = new HealthData();
        data.setStatus(HealthData.STATUS_UP);
        data.setVersion(buildProperties.getVersion());
        return ApiResponses.ok(data);
    }
}
