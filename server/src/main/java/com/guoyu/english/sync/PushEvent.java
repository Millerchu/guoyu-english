package com.guoyu.english.sync;

import java.time.Instant;

public record PushEvent(String eventId, Long childId, Instant occurredAt, String eventType, String stepId, String targetId) {
}
