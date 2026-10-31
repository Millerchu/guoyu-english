import type { SyncPushEvent, SyncPushRequest } from "./sync-types";

/** 稳定字段顺序，Push 字节数和实际请求体使用同一段 JSON。 */
export function encodeSyncPushRequest(request: SyncPushRequest): string {
  return JSON.stringify({
    schemaVersion: request.schemaVersion,
    streamEpoch: request.streamEpoch,
    events: request.events.map((event) => encodeEvent(event)),
  });
}

function encodeEvent(event: SyncPushEvent): SyncPushEvent {
  const encoded: SyncPushEvent = {
    eventId: event.eventId,
    eventType: "STEP_FINISHED",
    stepId: event.stepId,
  };
  if (event.targetId !== undefined) {
    return { ...encoded, targetId: event.targetId };
  }
  return encoded;
}
