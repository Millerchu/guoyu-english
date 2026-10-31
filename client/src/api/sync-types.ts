import type { LearningStepId, OutboxAckResult, SyncChange } from "../repositories/types";

/** 共享契约里的业务码。后续阶段追加前先改契约，这里不发明新码。 */
export const API_CODES = [
  "OK",
  "INVALID_ARGUMENT",
  "DEVICE_REVOKED",
  "EVENT_CONFLICT",
  "EVENT_ID_REUSED",
  "INTERNAL_ERROR",
] as const;

export type ApiCode = (typeof API_CODES)[number];

export interface ApiEnvelope<T> {
  readonly requestId: string;
  readonly code: ApiCode;
  readonly message: string;
  readonly data: T;
  readonly serverTime: string;
}

export const SYNC_SCHEMA_VERSION = 2;

/** 共享契约：最多 100 条且不超过 256 KiB。 */
export const SYNC_PUSH_MAX_EVENTS = 100;

export const SYNC_PUSH_MAX_BYTES = 256 * 1024;

export interface SyncPushEvent {
  readonly eventId: string;
  readonly eventType: "STEP_FINISHED";
  readonly stepId: LearningStepId;
  readonly targetId?: string;
}

export interface SyncPushRequest {
  readonly schemaVersion: typeof SYNC_SCHEMA_VERSION;
  readonly streamEpoch: string;
  readonly events: readonly SyncPushEvent[];
}

export interface SyncPushEventResult {
  readonly eventId: string;
  readonly status: OutboxAckResult;
  readonly code?: ApiCode;
}

export interface SyncPushResponseData {
  readonly results: readonly SyncPushEventResult[];
}

export interface SyncPullQuery {
  readonly childId: string;
  readonly streamEpoch: string;
  readonly cursor: string;
  readonly limit: number;
}

export interface SyncPullResponseData {
  readonly changes: readonly SyncChange[];
  readonly nextCursor: string;
  readonly hasMore: boolean;
}
