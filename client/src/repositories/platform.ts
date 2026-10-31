import { createAndroidSqliteExecutor, type PlusSqliteBridge } from "./android-sqlite";
import { StorageUnavailableError } from "./errors";
import { openIndexedDbDatabase } from "./indexeddb-database";
import { createLearningRepository } from "./learning-repository";
import type { LocalDatabase } from "./local-database";
import { openSqliteDatabase } from "./sqlite-database";
import type { LearningAction, LearningRepository, LessonSnapshot, OutboxAckResult, OutboxEntry, SyncChange } from "./types";

export interface PlatformHost {
  readonly indexedDB?: IDBFactory;
  readonly plus?: {
    readonly sqlite?: PlusSqliteBridge;
  };
}

export type PlatformKind = "android-sqlite" | "indexeddb";

/**
 * 同时存在 plus.sqlite 和 IndexedDB 时使用 SQLite。
 * 不能在 Android 上悄悄退回 IndexedDB。
 */
export function detectPlatformKind(host: PlatformHost): PlatformKind {
  if (isPlusSqlite(host.plus?.sqlite)) {
    return "android-sqlite";
  }
  if (host.indexedDB) {
    return "indexeddb";
  }
  throw new StorageUnavailableError();
}

export function readGlobalPlatformHost(): PlatformHost {
  const sqlite = readPlusSqlite(globalThis);
  if (typeof indexedDB === "undefined") {
    return sqlite ? { plus: { sqlite } } : {};
  }
  if (!sqlite) {
    return { indexedDB };
  }
  return { indexedDB, plus: { sqlite } };
}

export async function openPlatformDatabase(host: PlatformHost = readGlobalPlatformHost()): Promise<LocalDatabase> {
  const kind = detectPlatformKind(host);
  if (kind === "android-sqlite") {
    const sqlite = host.plus?.sqlite;
    if (!isPlusSqlite(sqlite)) {
      throw new StorageUnavailableError();
    }
    return openSqliteDatabase(createAndroidSqliteExecutor(sqlite));
  }
  if (!host.indexedDB) {
    throw new StorageUnavailableError();
  }
  return openIndexedDbDatabase({ factory: host.indexedDB });
}

export function createPlatformLearningRepository(host?: PlatformHost): LearningRepository {
  return createLazyRepository(() => openPlatformDatabase(host));
}

export const learningRepository: LearningRepository = createPlatformLearningRepository();

function createLazyRepository(open: () => Promise<LocalDatabase>): LearningRepository {
  let pending: Promise<LearningRepository> | null = null;
  const ready = (): Promise<LearningRepository> => {
    if (!pending) {
      pending = open()
        .then((db) => createLearningRepository(db))
        .catch((error: unknown) => {
          pending = null;
          throw error;
        });
    }
    return pending;
  };
  return {
    saveLearningAction(action: LearningAction): Promise<void> {
      return ready().then((repository) => repository.saveLearningAction(action));
    },
    listOutbox(limit: number): Promise<readonly OutboxEntry[]> {
      return ready().then((repository) => repository.listOutbox(limit));
    },
    ackOutbox(eventId: string, result: OutboxAckResult): Promise<void> {
      return ready().then((repository) => repository.ackOutbox(eventId, result));
    },
    savePullPage(changes: readonly SyncChange[], nextCursor: string): Promise<void> {
      return ready().then((repository) => repository.savePullPage(changes, nextCursor));
    },
    getLesson(lessonId: string, version: number): Promise<LessonSnapshot> {
      return ready().then((repository) => repository.getLesson(lessonId, version));
    },
  };
}

function readPlusSqlite(host: object): PlusSqliteBridge | undefined {
  if (!("plus" in host)) {
    return undefined;
  }
  const holder = host as { plus?: { sqlite?: unknown } };
  const sqlite = holder.plus?.sqlite;
  if (!isPlusSqlite(sqlite)) {
    return undefined;
  }
  return sqlite;
}

function isPlusSqlite(value: unknown): value is PlusSqliteBridge {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const candidate = value as {
    openDatabase?: unknown;
    executeSql?: unknown;
    selectSql?: unknown;
    transaction?: unknown;
    closeDatabase?: unknown;
  };
  return (
    typeof candidate.openDatabase === "function" &&
    typeof candidate.executeSql === "function" &&
    typeof candidate.selectSql === "function" &&
    typeof candidate.transaction === "function" &&
    typeof candidate.closeDatabase === "function"
  );
}
