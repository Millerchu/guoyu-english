package com.guoyu.english.common.web;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

import com.guoyu.english.common.api.ApiResponse;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void unexpectedErrorKeepsFiveFieldsAndHidesTheCause() throws Exception {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpected(
                new IllegalStateException("internal-detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.getMessage()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.getData()).isNull();
        assertThat(body.getRequestId()).isNotBlank();
        assertThat(body.getServerTime()).endsWith("Z");
        Instant.parse(body.getServerTime());

        JsonNode root = objectMapper.readTree(objectMapper.writeValueAsString(body));
        assertThat(root.has("requestId")).isTrue();
        assertThat(root.has("code")).isTrue();
        assertThat(root.has("message")).isTrue();
        assertThat(root.has("data")).isTrue();
        assertThat(root.has("serverTime")).isTrue();
        assertThat(root.get("data").isNull()).isTrue();
        assertThat(root.toString()).doesNotContain("internal-detail");
    }

    @Test
    void invalidArgumentUses400() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleInvalidArgument(
                new MissingServletRequestParameterException("childId", "String"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_ARGUMENT");
        assertThat(response.getBody().getMessage()).isEqualTo("INVALID_ARGUMENT");
    }
}
