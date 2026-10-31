import type { LearningStepId, OutboxAckResult } from "./types";

export const LOCAL_STORE_NAMES = [
  "progress",
  "outbox",
  "estimatedStar",
  "pullChange",
  "pullCursor",
  "lesson",
  "ackJournal",
] as const;

export type LocalStoreName = (typeof LOCAL_STORE_NAMES)[number];

export type LocalKey = string | number;

export interface ProgressRecord {
  readonly progressKey: string;
  readonly stepId: LearningStepId;
  readonly targetId: string | null;
  readonly eventId: string;
}

export interface OutboxRecord {
  readonly eventId: string;
  readonly seq: number;
  readonly stepId: LearningStepId;
  readonly targetId: string | null;
  readonly eventType: "STEP_FINISHED";
  readonly status: "PENDING" | OutboxAckResult;
  readonly createdAt: string;
}

export interface EstimatedStarRecord {
  readonly estimateKey: string;
  readonly rule: "SPEAK_STAR";
  readonly targetId: string;
  readonly eventId: string;
  readonly state: "PENDING";
}

export interface PullChangeRecord {
  readonly seq: number;
  readonly json: string;
}

export interface PullCursorRecord {
  readonly id: "local";
  readonly nextCursor: string;
}

export interface LessonRecord {
  readonly lessonKey: string;
  readonly lessonId: string;
  readonly version: number;
  readonly stepsJson: string;
}

export interface AckJournalRecord {
  readonly eventId: string;
  readonly result: OutboxAckResult;
  readonly stepId: LearningStepId;
  readonly targetId: string | null;
  readonly eventType: "STEP_FINISHED";
  readonly createdAt: string;
  readonly ackedAt: string;
}

export interface LocalRecords {
  progress: ProgressRecord;
  outbox: OutboxRecord;
  estimatedStar: EstimatedStarRecord;
  pullChange: PullChangeRecord;
  pullCursor: PullCursorRecord;
  lesson: LessonRecord;
  ackJournal: AckJournalRecord;
}

export type LocalTransactionMode = "readonly" | "readwrite";

export interface LocalTransaction {
  get<K extends LocalStoreName>(store: K, key: LocalKey): Promise<LocalRecords[K] | undefined>;
  put<K extends LocalStoreName>(store: K, value: LocalRecords[K]): Promise<void>;
  delete<K extends LocalStoreName>(store: K, key: LocalKey): Promise<void>;
  list<K extends LocalStoreName>(store: K): Promise<readonly LocalRecords[K][]>;
}

/**
 * H5 IndexedDB 与 Android SQLite 都实现这一接口。
 * transaction 在回调抛错时不提交已写入的行。
 */
export interface LocalDatabase {
  transaction<T>(mode: LocalTransactionMode, work: (tx: LocalTransaction) => Promise<T>): Promise<T>;
  close(): Promise<void>;
}

/** 测试注入一次写失败。生产调用不使用。 */
export interface WriteFaultInjection {
  armWriteFault(failOnMutate: number): void;
}

export const LOCAL_CURSOR_ID = "local";
