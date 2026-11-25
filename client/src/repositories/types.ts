/**
 * 客户端仓储签名。字段只使用共享契约已经点名的标识。
 * S09 实现这些方法；未实现时必须失败，不能假装已经保存。
 */

export type LearningStepId =
  | "step-listen"
  | "step-speak"
  | "step-recognize"
  | "step-read";

export interface LearningAction {
  readonly childId: number;
  readonly occurredAt: string;
  readonly eventId: string;
  readonly stepId: LearningStepId;
  readonly targetId?: string;
}

export interface EventSettlement {
  readonly streamEpoch: string;
  readonly commitCursor: string;
  readonly grantOutcomes: readonly { readonly grantKey: string; readonly status: "CONFIRMED"; readonly delta: number }[];
}

export type OutboxAckResult = "ACCEPTED" | "DUPLICATE" | "REJECTED";

/**
 * 待推送事件。eventId 由调用方在创建时给出，仓储和重试都不更换。
 * 保留动作发生时的儿童与 UTC 时间。
 */
export interface OutboxEntry {
  readonly childId: number;
  readonly occurredAt: string;
  readonly eventId: string;
  readonly eventType: "STEP_FINISHED";
  readonly stepId: LearningStepId;
  readonly targetId?: string;
}

/**
 * Pull 变更条目。共享契约只要求先把变更写成功，再推进游标，
 * 没有在客户端接口一节列出条目字段。这里不声明业务字段。
 * S09 按传入对象的 JSON 原样落盘，不另造字段。
 */
export type SyncChange = Record<string, never>;

export interface LessonStep {
  readonly stepId: LearningStepId;
}

export interface LessonSnapshot {
  readonly lessonId: string;
  readonly version: number;
  readonly steps: readonly LessonStep[];
}

export interface LearningRepository {
  saveLearningAction(action: LearningAction): Promise<void>;
  listOutbox(limit: number): Promise<readonly OutboxEntry[]>;
  ackOutbox(eventId: string, result: OutboxAckResult, settlement?: EventSettlement): Promise<void>;
  savePullPage(changes: readonly SyncChange[], nextCursor: string, scope?: string): Promise<void>;
  getLesson(lessonId: string, version: number): Promise<LessonSnapshot>;
}
