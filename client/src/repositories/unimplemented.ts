import type {
  LearningAction,
  LearningRepository,
  LessonSnapshot,
  OutboxAckResult,
  OutboxEntry,
  SyncChange,
} from "./types";

export class RepositoryNotImplementedError extends Error {
  readonly code = "NOT_IMPLEMENTED";
  readonly saved = false;

  constructor(methodName: string) {
    super(`${methodName} 尚未实现，没有保存`);
    this.name = "RepositoryNotImplementedError";
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

function notImplemented(methodName: string): Promise<never> {
  return Promise.reject(new RepositoryNotImplementedError(methodName));
}

export function createUnimplementedLearningRepository(): LearningRepository {
  return {
    saveLearningAction(_action: LearningAction): Promise<void> {
      return notImplemented("saveLearningAction");
    },
    listOutbox(_limit: number): Promise<readonly OutboxEntry[]> {
      return notImplemented("listOutbox");
    },
    ackOutbox(_eventId: string, _result: OutboxAckResult): Promise<void> {
      return notImplemented("ackOutbox");
    },
    savePullPage(_changes: readonly SyncChange[], _nextCursor: string): Promise<void> {
      return notImplemented("savePullPage");
    },
    getLesson(_lessonId: string, _version: number): Promise<LessonSnapshot> {
      return notImplemented("getLesson");
    },
  };
}

export const learningRepository: LearningRepository = createUnimplementedLearningRepository();
