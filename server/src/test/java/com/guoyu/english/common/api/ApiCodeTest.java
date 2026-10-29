package com.guoyu.english.common.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ApiCodeTest {

    @Test
    void namesStayOnTheSharedContract() {
        assertThat(ApiCode.values()).extracting(Enum::name).containsExactly(
                "OK",
                "INVALID_ARGUMENT",
                "DEVICE_REVOKED",
                "EVENT_CONFLICT",
                "EVENT_ID_REUSED",
                "INTERNAL_ERROR");
    }

    @Test
    void httpStatusFollowsTheDesignTable() {
        assertThat(ApiCode.OK.httpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(ApiCode.INVALID_ARGUMENT.httpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ApiCode.DEVICE_REVOKED.httpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ApiCode.EVENT_CONFLICT.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ApiCode.EVENT_ID_REUSED.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ApiCode.INTERNAL_ERROR.httpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
