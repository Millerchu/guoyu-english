package com.guoyu.english.sync;

import com.fasterxml.jackson.databind.JsonNode;

public record PullChange(long seq, String resource, String operation, JsonNode value) {
}
