import { InjectedWriteFaultError, StorageCorruptError, StorageUnavailableError } from "./errors";
import { seedContractLessons } from "./lesson-catalog";
import {
  LOCAL_CURSOR_ID,
  type LocalDatabase,
  type LocalKey,
  type LocalRecords,
  type LocalStoreName,
  type LocalTransaction,
  type LocalTransactionMode,
  type WriteFaultInjection,
} from "./local-database";
import { parseStoreValue } from "./record-parse";
import type { SqlExecutor, SqlRow, SqlScalar } from "./sql-executor";

/**
 * 语句保持 SQLite 3.18 可用（Android 8 / API 26），不用 UPSERT。
 * INSERT OR REPLACE 写入整行。
 */
export const SQLITE_SCHEMA: readonly string[] = [
  `CREATE TABLE IF NOT EXISTS progress (
    progress_key TEXT PRIMARY KEY NOT NULL,
    step_id TEXT NOT NULL,
    target_id TEXT,
    event_id TEXT NOT NULL
  )`,
  `CREATE TABLE IF NOT EXISTS outbox (
    event_id TEXT PRIMARY KEY NOT NULL,
    seq INTEGER NOT NULL UNIQUE,
    step_id TEXT NOT NULL,
    target_id TEXT,
    event_type TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TEXT NOT NULL
  )`,
  `CREATE TABLE IF NOT EXISTS estimated_star (
    estimate_key TEXT PRIMARY KEY NOT NULL,
    rule TEXT NOT NULL,
    target_id TEXT NOT NULL,
    event_id TEXT NOT NULL,
    state TEXT NOT NULL
  )`,
  `CREATE TABLE IF NOT EXISTS pull_change (
    seq INTEGER PRIMARY KEY NOT NULL,
    body TEXT NOT NULL
  )`,
  `CREATE TABLE IF NOT EXISTS pull_cursor (
    id TEXT PRIMARY KEY NOT NULL,
    next_cursor TEXT NOT NULL
  )`,
  `CREATE TABLE IF NOT EXISTS lesson (
    lesson_key TEXT PRIMARY KEY NOT NULL,
    lesson_id TEXT NOT NULL,
    version INTEGER NOT NULL,
    steps_json TEXT NOT NULL
  )`,
  `CREATE TABLE IF NOT EXISTS ack_journal (
    event_id TEXT PRIMARY KEY NOT NULL,
    result TEXT NOT NULL,
    step_id TEXT NOT NULL,
    target_id TEXT,
    event_type TEXT NOT NULL,
    created_at TEXT NOT NULL,
    acked_at TEXT NOT NULL
  )`,
];

export async function openSqliteDatabase(executor: SqlExecutor): Promise<LocalDatabase & WriteFaultInjection> {
  for (const statement of SQLITE_SCHEMA) {
    await executor.exec(statement);
  }
  const database = new SqliteLocalDatabase(executor);
  try {
    await seedContractLessons(database);
  } catch (error) {
    await database.close();
    throw error;
  }
  return database;
}

class SqliteLocalDatabase implements LocalDatabase, WriteFaultInjection {
  private tail: Promise<void> = Promise.resolve();
  private pendingFault: number | null = null;
  private closed = false;
  private readonly executor: SqlExecutor;

  constructor(executor: SqlExecutor) {
    this.executor = executor;
  }

  armWriteFault(failOnMutate: number): void {
    if (!Number.isSafeInteger(failOnMutate) || failOnMutate < 1) {
      throw new Error("failOnMutate 必须是正整数");
    }
    this.pendingFault = failOnMutate;
  }

  transaction<T>(mode: LocalTransactionMode, work: (tx: LocalTransaction) => Promise<T>): Promise<T> {
    if (this.closed) {
      return Promise.reject(new StorageUnavailableError());
    }
    const run = this.tail.then(() => this.runTransaction(mode, work));
    this.tail = run.then(
      () => undefined,
      () => undefined,
    );
    return run;
  }

  close(): Promise<void> {
    if (this.closed) {
      return Promise.resolve();
    }
    this.closed = true;
    return this.executor.close();
  }

  private takeFault(mode: LocalTransactionMode): number | null {
    if (mode !== "readwrite") {
      return null;
    }
    const fault = this.pendingFault;
    this.pendingFault = null;
    return fault;
  }

  private runTransaction<T>(
    mode: LocalTransactionMode,
    work: (tx: LocalTransaction) => Promise<T>,
  ): Promise<T> {
    const fault = this.takeFault(mode);
    let mutations = 0;
    const noteMutate = (): void => {
      mutations += 1;
      if (fault !== null && mutations === fault) {
        throw new InjectedWriteFaultError();
      }
    };
    const executor = this.executor;
    const localTx: LocalTransaction = {
      get: (store, key) => getRow(executor, store, key),
      put: async (store, value) => {
        const parsed = parseStoreValue(store, value);
        noteMutate();
        await putRow(executor, store, parsed);
      },
      delete: async (store, key) => {
        noteMutate();
        await deleteRow(executor, store, key);
      },
      list: (store) => listRows(executor, store),
    };
    return executor.transaction(() => work(localTx));
  }
}

async function getRow<K extends LocalStoreName>(
  executor: SqlExecutor,
  store: K,
  key: LocalKey,
): Promise<LocalRecords[K] | undefined> {
  const rows = await executor.query(selectOneSql(store), [keyValue(key)]);
  if (rows.length === 0) {
    return undefined;
  }
  if (rows.length > 1) {
    throw new StorageCorruptError(store);
  }
  return mapRow(store, rows[0]);
}

async function putRow<K extends LocalStoreName>(
  executor: SqlExecutor,
  store: K,
  value: LocalRecords[K],
): Promise<void> {
  const statement = insertSql(store, value);
  await executor.run(statement.sql, statement.params);
}

async function deleteRow(executor: SqlExecutor, store: LocalStoreName, key: LocalKey): Promise<void> {
  await executor.run(deleteSql(store), [keyValue(key)]);
}

async function listRows<K extends LocalStoreName>(
  executor: SqlExecutor,
  store: K,
): Promise<readonly LocalRecords[K][]> {
  const rows = await executor.query(selectAllSql(store));
  return rows.map((row) => mapRow(store, row));
}

function keyValue(key: LocalKey): SqlScalar {
  if (typeof key === "number") {
    if (!Number.isSafeInteger(key)) {
      throw new StorageCorruptError("sqlite");
    }
    return key;
  }
  return key;
}

function selectOneSql(store: LocalStoreName): string {
  switch (store) {
    case "progress":
      return "SELECT progress_key, step_id, target_id, event_id FROM progress WHERE progress_key = ?";
    case "outbox":
      return "SELECT event_id, seq, step_id, target_id, event_type, status, created_at FROM outbox WHERE event_id = ?";
    case "estimatedStar":
      return "SELECT estimate_key, rule, target_id, event_id, state FROM estimated_star WHERE estimate_key = ?";
    case "pullChange":
      return "SELECT seq, body FROM pull_change WHERE seq = ?";
    case "pullCursor":
      return "SELECT id, next_cursor FROM pull_cursor WHERE id = ?";
    case "lesson":
      return "SELECT lesson_key, lesson_id, version, steps_json FROM lesson WHERE lesson_key = ?";
    case "ackJournal":
      return "SELECT event_id, result, step_id, target_id, event_type, created_at, acked_at FROM ack_journal WHERE event_id = ?";
    default: {
      const neverStore: never = store;
      throw new StorageCorruptError(String(neverStore));
    }
  }
}

function selectAllSql(store: LocalStoreName): string {
  switch (store) {
    case "progress":
      return "SELECT progress_key, step_id, target_id, event_id FROM progress";
    case "outbox":
      return "SELECT event_id, seq, step_id, target_id, event_type, status, created_at FROM outbox ORDER BY seq ASC";
    case "estimatedStar":
      return "SELECT estimate_key, rule, target_id, event_id, state FROM estimated_star";
    case "pullChange":
      return "SELECT seq, body FROM pull_change ORDER BY seq ASC";
    case "pullCursor":
      return "SELECT id, next_cursor FROM pull_cursor";
    case "lesson":
      return "SELECT lesson_key, lesson_id, version, steps_json FROM lesson";
    case "ackJournal":
      return "SELECT event_id, result, step_id, target_id, event_type, created_at, acked_at FROM ack_journal";
    default: {
      const neverStore: never = store;
      throw new StorageCorruptError(String(neverStore));
    }
  }
}

function deleteSql(store: LocalStoreName): string {
  switch (store) {
    case "progress":
      return "DELETE FROM progress WHERE progress_key = ?";
    case "outbox":
      return "DELETE FROM outbox WHERE event_id = ?";
    case "estimatedStar":
      return "DELETE FROM estimated_star WHERE estimate_key = ?";
    case "pullChange":
      return "DELETE FROM pull_change WHERE seq = ?";
    case "pullCursor":
      return "DELETE FROM pull_cursor WHERE id = ?";
    case "lesson":
      return "DELETE FROM lesson WHERE lesson_key = ?";
    case "ackJournal":
      return "DELETE FROM ack_journal WHERE event_id = ?";
    default: {
      const neverStore: never = store;
      throw new StorageCorruptError(String(neverStore));
    }
  }
}

function insertSql(
  store: LocalStoreName,
  value: LocalRecords[LocalStoreName],
): { sql: string; params: readonly SqlScalar[] } {
  switch (store) {
    case "progress": {
      const row = parseStoreValue("progress", value);
      return {
        sql: "INSERT OR REPLACE INTO progress (progress_key, step_id, target_id, event_id) VALUES (?, ?, ?, ?)",
        params: [row.progressKey, row.stepId, row.targetId, row.eventId],
      };
    }
    case "outbox": {
      const row = parseStoreValue("outbox", value);
      return {
        sql: `INSERT OR REPLACE INTO outbox
          (event_id, seq, step_id, target_id, event_type, status, created_at)
          VALUES (?, ?, ?, ?, ?, ?, ?)`,
        params: [row.eventId, row.seq, row.stepId, row.targetId, row.eventType, row.status, row.createdAt],
      };
    }
    case "estimatedStar": {
      const row = parseStoreValue("estimatedStar", value);
      return {
        sql: `INSERT OR REPLACE INTO estimated_star
          (estimate_key, rule, target_id, event_id, state)
          VALUES (?, ?, ?, ?, ?)`,
        params: [row.estimateKey, row.rule, row.targetId, row.eventId, row.state],
      };
    }
    case "pullChange": {
      const row = parseStoreValue("pullChange", value);
      return {
        sql: "INSERT OR REPLACE INTO pull_change (seq, body) VALUES (?, ?)",
        params: [row.seq, row.json],
      };
    }
    case "pullCursor": {
      const row = parseStoreValue("pullCursor", value);
      if (row.id !== LOCAL_CURSOR_ID) {
        throw new StorageCorruptError("pullCursor");
      }
      return {
        sql: "INSERT OR REPLACE INTO pull_cursor (id, next_cursor) VALUES (?, ?)",
        params: [row.id, row.nextCursor],
      };
    }
    case "lesson": {
      const row = parseStoreValue("lesson", value);
      return {
        sql: "INSERT OR REPLACE INTO lesson (lesson_key, lesson_id, version, steps_json) VALUES (?, ?, ?, ?)",
        params: [row.lessonKey, row.lessonId, row.version, row.stepsJson],
      };
    }
    case "ackJournal": {
      const row = parseStoreValue("ackJournal", value);
      return {
        sql: `INSERT OR REPLACE INTO ack_journal
          (event_id, result, step_id, target_id, event_type, created_at, acked_at)
          VALUES (?, ?, ?, ?, ?, ?, ?)`,
        params: [row.eventId, row.result, row.stepId, row.targetId, row.eventType, row.createdAt, row.ackedAt],
      };
    }
    default: {
      const neverStore: never = store;
      throw new StorageCorruptError(String(neverStore));
    }
  }
}

function mapRow<K extends LocalStoreName>(store: K, row: SqlRow): LocalRecords[K] {
  switch (store) {
    case "progress":
      return parseStoreValue(store, {
        progressKey: requiredText(row, "progress_key"),
        stepId: requiredText(row, "step_id"),
        targetId: nullableText(row, "target_id"),
        eventId: requiredText(row, "event_id"),
      });
    case "outbox":
      return parseStoreValue(store, {
        eventId: requiredText(row, "event_id"),
        seq: requiredInt(row, "seq"),
        stepId: requiredText(row, "step_id"),
        targetId: nullableText(row, "target_id"),
        eventType: requiredText(row, "event_type"),
        status: requiredText(row, "status"),
        createdAt: requiredText(row, "created_at"),
      });
    case "estimatedStar":
      return parseStoreValue(store, {
        estimateKey: requiredText(row, "estimate_key"),
        rule: requiredText(row, "rule"),
        targetId: requiredText(row, "target_id"),
        eventId: requiredText(row, "event_id"),
        state: requiredText(row, "state"),
      });
    case "pullChange":
      return parseStoreValue(store, {
        seq: requiredInt(row, "seq"),
        json: requiredText(row, "body"),
      });
    case "pullCursor":
      return parseStoreValue(store, {
        id: requiredText(row, "id"),
        nextCursor: requiredText(row, "next_cursor"),
      });
    case "lesson":
      return parseStoreValue(store, {
        lessonKey: requiredText(row, "lesson_key"),
        lessonId: requiredText(row, "lesson_id"),
        version: requiredInt(row, "version"),
        stepsJson: requiredText(row, "steps_json"),
      });
    case "ackJournal":
      return parseStoreValue(store, {
        eventId: requiredText(row, "event_id"),
        result: requiredText(row, "result"),
        stepId: requiredText(row, "step_id"),
        targetId: nullableText(row, "target_id"),
        eventType: requiredText(row, "event_type"),
        createdAt: requiredText(row, "created_at"),
        ackedAt: requiredText(row, "acked_at"),
      });
    default: {
      const neverStore: never = store;
      throw new StorageCorruptError(String(neverStore));
    }
  }
}

function requiredText(row: SqlRow, key: string): string {
  const value = row[key];
  if (typeof value !== "string") {
    throw new StorageCorruptError("sqlite");
  }
  return value;
}

function nullableText(row: SqlRow, key: string): string | null {
  if (!Object.prototype.hasOwnProperty.call(row, key)) {
    throw new StorageCorruptError("sqlite");
  }
  const value = row[key];
  if (value === null) {
    return null;
  }
  if (typeof value !== "string") {
    throw new StorageCorruptError("sqlite");
  }
  return value;
}

function requiredInt(row: SqlRow, key: string): number {
  const value = row[key];
  if (typeof value !== "number" || !Number.isSafeInteger(value)) {
    throw new StorageCorruptError("sqlite");
  }
  return value;
}
