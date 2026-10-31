import { InvalidArgumentError } from "../repositories/errors";
import { parseToken } from "../repositories/record-parse";
import type { OutboxEntry } from "../repositories/types";
import { encodeSyncPushRequest } from "../api/sync-json";
import {
  SYNC_PUSH_MAX_BYTES,
  SYNC_PUSH_MAX_EVENTS,
  SYNC_SCHEMA_VERSION,
  type SyncPushEvent,
  type SyncPushRequest,
} from "../api/sync-types";

export interface SyncPushLimits {
  readonly maxEvents?: number;
  readonly maxBytes?: number;
}

export function syncPushRequestBytes(request: SyncPushRequest): number {
  return new TextEncoder().encode(encodeSyncPushRequest(request)).length;
}

/**
 * 按 100 条 / 256 KiB 拆批。事件对象原样带上已有 eventId，不新建。
 * 单条已经超过上限时抛错，调用方保留原来的 eventId。
 */
export function splitSyncPushRequests(
  entries: readonly OutboxEntry[],
  streamEpoch: string,
  limits: SyncPushLimits = {},
): readonly SyncPushRequest[] {
  const epoch = parseToken(streamEpoch, "streamEpoch", 80);
  const maxEvents = limits.maxEvents ?? SYNC_PUSH_MAX_EVENTS;
  const maxBytes = limits.maxBytes ?? SYNC_PUSH_MAX_BYTES;
  if (!Number.isSafeInteger(maxEvents) || maxEvents < 1 || !Number.isSafeInteger(maxBytes) || maxBytes < 1) {
    throw new InvalidArgumentError("limit");
  }
  const batches: SyncPushRequest[] = [];
  let current: SyncPushEvent[] = [];
  const flush = (): void => {
    if (current.length === 0) {
      return;
    }
    batches.push({
      schemaVersion: SYNC_SCHEMA_VERSION,
      streamEpoch: epoch,
      events: current,
    });
    current = [];
  };
  for (const entry of entries) {
    const event = toPushEvent(entry);
    const candidate = [...current, event];
    const tooMany = candidate.length > maxEvents;
    const tooLarge = syncPushRequestBytes(requestFrom(epoch, candidate)) > maxBytes;
    if (current.length > 0 && (tooMany || tooLarge)) {
      flush();
    }
    const alone = requestFrom(epoch, [event]);
    if (syncPushRequestBytes(alone) > maxBytes) {
      throw new InvalidArgumentError("events");
    }
    current = [...current, event];
  }
  flush();
  return batches;
}

function requestFrom(streamEpoch: string, events: readonly SyncPushEvent[]): SyncPushRequest {
  return {
    schemaVersion: SYNC_SCHEMA_VERSION,
    streamEpoch,
    events,
  };
}

export function toPushEvent(entry: OutboxEntry): SyncPushEvent {
  const event: SyncPushEvent = {
    eventId: entry.eventId,
    eventType: "STEP_FINISHED",
    stepId: entry.stepId,
  };
  if (entry.targetId !== undefined) {
    return { ...event, targetId: entry.targetId };
  }
  return event;
}
