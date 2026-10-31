import { InvalidArgumentError, StorageCorruptError } from "./errors";
import type {
  AckJournalRecord,
  EstimatedStarRecord,
  LessonRecord,
  LocalRecords,
  LocalStoreName,
  OutboxRecord,
  ProgressRecord,
  PullChangeRecord,
  PullCursorRecord,
} from "./local-database";
import type { LearningStepId, OutboxAckResult } from "./types";

const STEP_IDS: readonly LearningStepId[] = [
  "step-listen",
  "step-speak",
  "step-recognize",
  "step-read",
];

const ACK_RESULTS: readonly OutboxAckResult[] = ["ACCEPTED", "DUPLICATE", "REJECTED"];

const OUTBOX_STATUSES: readonly OutboxRecord["status"][] = ["PENDING", ...ACK_RESULTS];

const TOKEN = /^[A-Za-z0-9._:-]+$/;

const UTC_TIME = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,3})?Z$/;

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function parseToken(value: unknown, field: string, maxLength: number): string {
  if (typeof value !== "string" || value.length === 0 || value.length > maxLength || !TOKEN.test(value)) {
    throw new InvalidArgumentError(field);
  }
  return value;
}

export function parseOptionalToken(value: unknown, field: string, maxLength: number): string | null {
  if (value === undefined || value === null || value === "") {
    return null;
  }
  return parseToken(value, field, maxLength);
}

export function parseLimit(value: unknown): number {
  if (typeof value !== "number" || !Number.isSafeInteger(value) || value < 0) {
    throw new InvalidArgumentError("limit");
  }
  return value;
}

export function parseVersion(value: unknown): number {
  if (typeof value !== "number" || !Number.isSafeInteger(value) || value < 1 || value > 10000) {
    throw new InvalidArgumentError("version");
  }
  return value;
}

export function parseCursor(value: unknown): string {
  return parseToken(value, "cursor", 256);
}

export function isLearningStepId(value: string): value is LearningStepId {
  return STEP_IDS.some((stepId) => stepId === value);
}

function isOutboxStatus(value: unknown): value is OutboxRecord["status"] {
  return typeof value === "string" && OUTBOX_STATUSES.some((item) => item === value);
}

function isAckResult(value: unknown): value is OutboxAckResult {
  return typeof value === "string" && ACK_RESULTS.some((item) => item === value);
}

function readRecord(value: unknown, store: string): Record<string, unknown> {
  if (!isRecord(value)) {
    throw new StorageCorruptError(store);
  }
  return value;
}

function readToken(source: Record<string, unknown>, key: string, store: string, maxLength: number): string {
  const value = source[key];
  if (typeof value !== "string" || value.length === 0 || value.length > maxLength || !TOKEN.test(value)) {
    throw new StorageCorruptError(store);
  }
  return value;
}

function readNullableToken(
  source: Record<string, unknown>,
  key: string,
  store: string,
  maxLength: number,
): string | null {
  const value = source[key];
  if (value === null) {
    return null;
  }
  if (typeof value !== "string" || value.length === 0 || value.length > maxLength || !TOKEN.test(value)) {
    throw new StorageCorruptError(store);
  }
  return value;
}

function readStepId(source: Record<string, unknown>, store: string): LearningStepId {
  const value = source.stepId;
  if (typeof value !== "string" || !isLearningStepId(value)) {
    throw new StorageCorruptError(store);
  }
  return value;
}

function readPositiveInt(source: Record<string, unknown>, key: string, store: string): number {
  const value = source[key];
  if (typeof value !== "number" || !Number.isSafeInteger(value) || value < 1) {
    throw new StorageCorruptError(store);
  }
  return value;
}

function readUtcTime(source: Record<string, unknown>, key: string, store: string): string {
  const value = source[key];
  if (typeof value !== "string" || !UTC_TIME.test(value)) {
    throw new StorageCorruptError(store);
  }
  return value;
}

export function progressKeyOf(stepId: LearningStepId, targetId: string | null): string {
  return targetId === null ? stepId : `${stepId}:${targetId}`;
}

export function lessonKeyOf(lessonId: string, version: number): string {
  return `${lessonId}:${version}`;
}

export function speakStarKey(targetId: string): string {
  return `SPEAK_STAR:${targetId}`;
}

export function parseProgressRecord(value: unknown): ProgressRecord {
  const source = readRecord(value, "progress");
  const stepId = readStepId(source, "progress");
  const targetId = readNullableToken(source, "targetId", "progress", 80);
  const eventId = readToken(source, "eventId", "progress", 80);
  const progressKey = readToken(source, "progressKey", "progress", 180);
  if (progressKey !== progressKeyOf(stepId, targetId)) {
    throw new StorageCorruptError("progress");
  }
  return { progressKey, stepId, targetId, eventId };
}

export function parseOutboxRecord(value: unknown): OutboxRecord {
  const source = readRecord(value, "outbox");
  const status = source.status;
  if (!isOutboxStatus(status)) {
    throw new StorageCorruptError("outbox");
  }
  if (source.eventType !== "STEP_FINISHED") {
    throw new StorageCorruptError("outbox");
  }
  return {
    eventId: readToken(source, "eventId", "outbox", 80),
    seq: readPositiveInt(source, "seq", "outbox"),
    stepId: readStepId(source, "outbox"),
    targetId: readNullableToken(source, "targetId", "outbox", 80),
    eventType: "STEP_FINISHED",
    status,
    createdAt: readUtcTime(source, "createdAt", "outbox"),
  };
}

export function parseEstimatedStarRecord(value: unknown): EstimatedStarRecord {
  const source = readRecord(value, "estimatedStar");
  if (source.rule !== "SPEAK_STAR" || source.state !== "PENDING") {
    throw new StorageCorruptError("estimatedStar");
  }
  const targetId = readToken(source, "targetId", "estimatedStar", 80);
  const estimateKey = readToken(source, "estimateKey", "estimatedStar", 120);
  if (estimateKey !== speakStarKey(targetId)) {
    throw new StorageCorruptError("estimatedStar");
  }
  return {
    estimateKey,
    rule: "SPEAK_STAR",
    targetId,
    eventId: readToken(source, "eventId", "estimatedStar", 80),
    state: "PENDING",
  };
}

export function parsePullChangeRecord(value: unknown): PullChangeRecord {
  const source = readRecord(value, "pullChange");
  const json = source.json;
  if (typeof json !== "string" || json.length === 0 || json.length > 256 * 1024) {
    throw new StorageCorruptError("pullChange");
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(json) as unknown;
  } catch {
    throw new StorageCorruptError("pullChange");
  }
  if (!isRecord(parsed)) {
    throw new StorageCorruptError("pullChange");
  }
  return {
    seq: readPositiveInt(source, "seq", "pullChange"),
    json,
  };
}

export function parsePullCursorRecord(value: unknown): PullCursorRecord {
  const source = readRecord(value, "pullCursor");
  if (source.id !== "local") {
    throw new StorageCorruptError("pullCursor");
  }
  return {
    id: "local",
    nextCursor: readToken(source, "nextCursor", "pullCursor", 256),
  };
}

export function parseLessonRecord(value: unknown): LessonRecord {
  const source = readRecord(value, "lesson");
  const lessonId = readToken(source, "lessonId", "lesson", 80);
  const version = readPositiveInt(source, "version", "lesson");
  const lessonKey = readToken(source, "lessonKey", "lesson", 120);
  if (lessonKey !== lessonKeyOf(lessonId, version)) {
    throw new StorageCorruptError("lesson");
  }
  const stepsJson = source.stepsJson;
  if (typeof stepsJson !== "string") {
    throw new StorageCorruptError("lesson");
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(stepsJson) as unknown;
  } catch {
    throw new StorageCorruptError("lesson");
  }
  if (!Array.isArray(parsed) || parsed.length === 0) {
    throw new StorageCorruptError("lesson");
  }
  for (const step of parsed) {
    if (typeof step !== "string" || !isLearningStepId(step)) {
      throw new StorageCorruptError("lesson");
    }
  }
  return { lessonKey, lessonId, version, stepsJson };
}

export function parseAckJournalRecord(value: unknown): AckJournalRecord {
  const source = readRecord(value, "ackJournal");
  const result = source.result;
  if (!isAckResult(result)) {
    throw new StorageCorruptError("ackJournal");
  }
  if (source.eventType !== "STEP_FINISHED") {
    throw new StorageCorruptError("ackJournal");
  }
  return {
    eventId: readToken(source, "eventId", "ackJournal", 80),
    result,
    stepId: readStepId(source, "ackJournal"),
    targetId: readNullableToken(source, "targetId", "ackJournal", 80),
    eventType: "STEP_FINISHED",
    createdAt: readUtcTime(source, "createdAt", "ackJournal"),
    ackedAt: readUtcTime(source, "ackedAt", "ackJournal"),
  };
}

export function parseStoreValue<K extends LocalStoreName>(store: K, value: unknown): LocalRecords[K] {
  switch (store) {
    case "progress":
      return parseProgressRecord(value) as LocalRecords[K];
    case "outbox":
      return parseOutboxRecord(value) as LocalRecords[K];
    case "estimatedStar":
      return parseEstimatedStarRecord(value) as LocalRecords[K];
    case "pullChange":
      return parsePullChangeRecord(value) as LocalRecords[K];
    case "pullCursor":
      return parsePullCursorRecord(value) as LocalRecords[K];
    case "lesson":
      return parseLessonRecord(value) as LocalRecords[K];
    case "ackJournal":
      return parseAckJournalRecord(value) as LocalRecords[K];
    default: {
      const neverStore: never = store;
      throw new StorageCorruptError(String(neverStore));
    }
  }
}
