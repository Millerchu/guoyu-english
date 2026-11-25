package com.guoyu.english.sync;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;

public record PushBatch(Integer schemaVersion, String streamEpoch, List<JsonNode> events) {
}
