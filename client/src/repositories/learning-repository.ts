import {
  AckConflictError,
  EventIdReusedError,
  InjectedWriteFaultError,
  InvalidArgumentError,
  InvalidLearningActionError,
  LearningActionNotSavedError,
  LessonNotFoundError,
  OutboxEventNotFoundError,
  PullPageNotSavedError,
  StorageReadError,
} from "./errors";
import { LOCAL_CURSOR_ID, type LocalDatabase, type LocalTransaction } from "./local-database";
import {
  isLearningStepId,
  isRecord,
  parseCursor,
  parseLimit,
  parseOptionalToken,
  parseToken,
  lessonKeyOf,
  parseVersion,
  progressKeyOf,
  speakStarKey,
} from "./record-parse";
import type {
  LearningAction,
  LearningRepository,
  LearningStepId,
  LessonSnapshot,
  OutboxAckResult,
  OutboxEntry,
  SyncChange,
} from "./types";

const PULL_PAGE_MAX_CHANGES = 500;
const PULL_CHANGE_MAX_JSON_BYTES = 256 * 1024;

interface ParsedAction {
  readonly eventId: string;
  readonly stepId: LearningStepId;
  readonly targetId: string | null;
}

interface StoredPayload {
  readonly stepId: LearningStepId;
  readonly targetId: string | null;
  readonly eventType: "STEP_FINISHED";
}

export interface LearningRepositoryOptions {
  readonly now?: () => Date;
}

export function createLearningRepository(
  db: LocalDatabase,
  options: LearningRepositoryOptions = {},
): LearningRepository {
  const now = options.now ?? (() => new Date());

  return {
    async saveLearningAction(action: LearningAction): Promise<void> {
      const parsed = parseLearningAction(action);
      try {
        await db.transaction("readwrite", async (tx) => {
          const existing = await tx.get("outbox", parsed.eventId);
          const journal = await tx.get("ackJournal", parsed.eventId);
          if (existing && !samePayload(existing, parsed)) {
            throw new EventIdReusedError();
          }
          if (journal && !samePayload(journal, parsed)) {
            throw new EventIdReusedError();
          }
          if (existing || journal) {
            return;
          }
          const progressKey = progressKeyOf(parsed.stepId, parsed.targetId);
          const priorProgress = await tx.get("progress", progressKey);
          if (!priorProgress) {
            await tx.put("progress", {
              progressKey,
              stepId: parsed.stepId,
              targetId: parsed.targetId,
              eventId: parsed.eventId,
            });
          }
          const seq = (await maxOutboxSeq(tx)) + 1;
          await tx.put("outbox", {
            eventId: parsed.eventId,
            seq,
            stepId: parsed.stepId,
            targetId: parsed.targetId,
            eventType: "STEP_FINISHED",
            status: "PENDING",
            createdAt: now().toISOString(),
          });
          if (parsed.stepId === "step-speak" && parsed.targetId) {
            const estimateKey = speakStarKey(parsed.targetId);
            const priorStar = await tx.get("estimatedStar", estimateKey);
            if (!priorStar) {
              await tx.put("estimatedStar", {
                estimateKey,
                rule: "SPEAK_STAR",
                targetId: parsed.targetId,
                eventId: parsed.eventId,
                state: "PENDING",
              });
            }
          }
        });
      } catch (error) {
        throw wrapSaveError(error);
      }
    },

    async listOutbox(limit: number): Promise<readonly OutboxEntry[]> {
      const parsedLimit = parseLimit(limit);
      let rows;
      try {
        rows = await db.transaction("readonly", (tx) => tx.list("outbox"));
      } catch (error) {
        throw new StorageReadError(error);
      }
      return rows
        .filter((row) => row.status === "PENDING")
        .sort((left, right) => left.seq - right.seq || left.eventId.localeCompare(right.eventId))
        .slice(0, parsedLimit)
        .map(toOutboxEntry);
    },

    async ackOutbox(eventId: string, result: OutboxAckResult): Promise<void> {
      const parsedId = parseToken(eventId, "eventId", 80);
      const parsedResult = parseAckResult(result);
      try {
        await db.transaction("readwrite", async (tx) => {
          const row = await tx.get("outbox", parsedId);
          const journal = await tx.get("ackJournal", parsedId);
          if (journal && compatibleAck(journal.result, parsedResult)) {
            return;
          }
          if (journal) {
            throw new AckConflictError();
          }
          if (!row || row.status !== "PENDING") {
            throw new OutboxEventNotFoundError();
          }
          await tx.put("ackJournal", {
            eventId: row.eventId,
            result: parsedResult,
            stepId: row.stepId,
            targetId: row.targetId,
            eventType: row.eventType,
            createdAt: row.createdAt,
            ackedAt: now().toISOString(),
          });
          if (parsedResult === "REJECTED") {
            await tx.put("outbox", { ...row, status: "REJECTED" });
            return;
          }
          await tx.delete("outbox", row.eventId);
        });
      } catch (error) {
        throw wrapAckError(error);
      }
    },

    async savePullPage(changes: readonly SyncChange[], nextCursor: string): Promise<void> {
      const cursor = parseCursor(nextCursor);
      const page = encodePullChanges(changes);
      try {
        await db.transaction("readwrite", async (tx) => {
          let seq = await maxPullSeq(tx);
          for (const json of page) {
            seq += 1;
            await tx.put("pullChange", { seq, json });
          }
          await tx.put("pullCursor", { id: LOCAL_CURSOR_ID, nextCursor: cursor });
        });
      } catch (error) {
        throw wrapPullError(error);
      }
    },

    async getLesson(lessonId: string, version: number): Promise<LessonSnapshot> {
      const parsedId = parseToken(lessonId, "lessonId", 80);
      const parsedVersion = parseVersion(version);
      let row;
      try {
        row = await db.transaction("readonly", (tx) => tx.get("lesson", lessonKeyOf(parsedId, parsedVersion)));
      } catch (error) {
        throw new StorageReadError(error);
      }
      if (!row) {
        throw new LessonNotFoundError();
      }
      const steps = decodeLessonSteps(row.stepsJson);
      return {
        lessonId: row.lessonId,
        version: row.version,
        steps,
      };
    },
  };
}

function parseLearningAction(action: LearningAction): ParsedAction {
  if (!isRecord(action)) {
    throw new InvalidLearningActionError("学习动作无效，没有保存");
  }
  const eventId = parseToken(action.eventId, "eventId", 80);
  const stepValue = action.stepId;
  if (typeof stepValue !== "string" || !isLearningStepId(stepValue)) {
    throw new InvalidLearningActionError("步骤无效，没有保存");
  }
  const targetId = parseOptionalToken(action.targetId, "targetId", 80);
  if (stepValue === "step-speak" && targetId === null) {
    throw new InvalidLearningActionError("跟我说缺少句子，没有保存");
  }
  return { eventId, stepId: stepValue, targetId };
}

function parseAckResult(result: OutboxAckResult): OutboxAckResult {
  if (result !== "ACCEPTED" && result !== "DUPLICATE" && result !== "REJECTED") {
    throw new InvalidArgumentError("result");
  }
  return result;
}

function samePayload(record: StoredPayload, action: ParsedAction): boolean {
  return record.eventType === "STEP_FINISHED" && record.stepId === action.stepId && record.targetId === action.targetId;
}

function compatibleAck(saved: OutboxAckResult, incoming: OutboxAckResult): boolean {
  if (saved === incoming) {
    return true;
  }
  return isSuccessAck(saved) && isSuccessAck(incoming);
}

function isSuccessAck(result: OutboxAckResult): boolean {
  return result === "ACCEPTED" || result === "DUPLICATE";
}

function toOutboxEntry(record: {
  eventId: string;
  stepId: LearningStepId;
  targetId: string | null;
}): OutboxEntry {
  const entry: OutboxEntry = {
    eventId: record.eventId,
    eventType: "STEP_FINISHED",
    stepId: record.stepId,
  };
  if (record.targetId !== null) {
    return { ...entry, targetId: record.targetId };
  }
  return entry;
}

async function maxOutboxSeq(tx: LocalTransaction): Promise<number> {
  const rows = await tx.list("outbox");
  return rows.reduce((max, row) => (row.seq > max ? row.seq : max), 0);
}

async function maxPullSeq(tx: LocalTransaction): Promise<number> {
  const rows = await tx.list("pullChange");
  return rows.reduce((max, row) => (row.seq > max ? row.seq : max), 0);
}

function encodePullChanges(changes: readonly SyncChange[]): readonly string[] {
  if (!Array.isArray(changes) || changes.length > PULL_PAGE_MAX_CHANGES) {
    throw new InvalidArgumentError("changes");
  }
  return changes.map((change) => {
    if (!isRecord(change)) {
      throw new InvalidArgumentError("changes");
    }
    assertJsonData(change);
    const json = JSON.stringify(change);
    if (typeof json !== "string" || json.length === 0 || json.length > PULL_CHANGE_MAX_JSON_BYTES) {
      throw new InvalidArgumentError("changes");
    }
    return json;
  });
}

function assertJsonData(value: unknown): void {
  if (value === null || typeof value === "string" || typeof value === "boolean") {
    return;
  }
  if (typeof value === "number") {
    if (!Number.isFinite(value)) {
      throw new InvalidArgumentError("changes");
    }
    return;
  }
  if (Array.isArray(value)) {
    for (const item of value) {
      assertJsonData(item);
    }
    return;
  }
  if (isRecord(value)) {
    for (const key of Object.keys(value)) {
      assertJsonData(value[key]);
    }
    return;
  }
  throw new InvalidArgumentError("changes");
}

function decodeLessonSteps(stepsJson: string): LessonSnapshot["steps"] {
  const parsed = JSON.parse(stepsJson) as unknown;
  if (!Array.isArray(parsed)) {
    throw new StorageReadError(new LessonNotFoundError());
  }
  return parsed.map((stepId) => {
    if (typeof stepId !== "string" || !isLearningStepId(stepId)) {
      throw new StorageReadError(new LessonNotFoundError());
    }
    return { stepId };
  });
}

function wrapSaveError(error: unknown): Error {
  if (
    error instanceof EventIdReusedError ||
    error instanceof InvalidLearningActionError ||
    error instanceof InvalidArgumentError ||
    error instanceof LearningActionNotSavedError
  ) {
    return error;
  }
  return new LearningActionNotSavedError(error);
}

function wrapAckError(error: unknown): Error {
  if (
    error instanceof AckConflictError ||
    error instanceof OutboxEventNotFoundError ||
    error instanceof InvalidArgumentError ||
    error instanceof LearningActionNotSavedError
  ) {
    return error;
  }
  return new LearningActionNotSavedError(error);
}

function wrapPullError(error: unknown): Error {
  if (error instanceof InvalidArgumentError || error instanceof PullPageNotSavedError) {
    return error;
  }
  if (error instanceof InjectedWriteFaultError) {
    return new PullPageNotSavedError(error);
  }
  return new PullPageNotSavedError(error);
}
