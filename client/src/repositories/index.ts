export type { SaveFailureFeedback } from "./save-feedback";
export { feedbackForSaveFailure } from "./save-feedback";
export type {
  LearningAction,
  LearningRepository,
  LearningStepId,
  LessonSnapshot,
  LessonStep,
  OutboxAckResult,
  OutboxEntry,
  SyncChange,
} from "./types";
export { RepositoryNotImplementedError, createUnimplementedLearningRepository } from "./unimplemented";
export { createLearningRepository } from "./learning-repository";
export type { LearningRepositoryOptions } from "./learning-repository";
export { openIndexedDbDatabase } from "./indexeddb-database";
export { openSqliteDatabase } from "./sqlite-database";
export {
  ANDROID_SQLITE_DATABASE_NAME,
  ANDROID_SQLITE_DATABASE_PATH,
  ANDROID_SQLITE_DEVICE_VERIFIED,
  createAndroidSqliteExecutor,
} from "./android-sqlite";
export type { PlusSqliteBridge } from "./android-sqlite";
export type { SqlExecutor } from "./sql-executor";
export { bindSql } from "./sql-executor";
export {
  createPlatformLearningRepository,
  detectPlatformKind,
  learningRepository,
  openPlatformDatabase,
} from "./platform";
export type { PlatformHost, PlatformKind } from "./platform";
export type { LocalDatabase, WriteFaultInjection } from "./local-database";
