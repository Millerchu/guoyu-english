import {
  bindSql,
  toSqlRow,
  type SqlExecutor,
  type SqlRow,
  type SqlScalar,
} from "./sql-executor";

/**
 * Android SQLite 适配。
 * 本文件没有在 Android 真机、模拟器或 HBuilderX 打包里执行过。
 * 单元测试注入 PlusSqliteBridge，底下用同一套 SQL，不能当作真机验收。
 */
export const ANDROID_SQLITE_DEVICE_VERIFIED = false;

export const ANDROID_SQLITE_DATABASE_NAME = "guoyu-english";

/** plus 的应用私有目录，不是 NAS 绝对路径。 */
export const ANDROID_SQLITE_DATABASE_PATH = "_doc/guoyu-english.db";

export interface PlusSqliteError {
  readonly code?: number | string;
  readonly message?: string;
}

export interface PlusSqliteOpenOptions {
  readonly name: string;
  readonly path: string;
  readonly success: () => void;
  readonly fail: (error: PlusSqliteError) => void;
}

export interface PlusSqliteExecuteOptions {
  readonly name: string;
  readonly sql: string;
  readonly success: () => void;
  readonly fail: (error: PlusSqliteError) => void;
}

export interface PlusSqliteSelectOptions {
  readonly name: string;
  readonly sql: string;
  readonly success: (rows: readonly unknown[]) => void;
  readonly fail: (error: PlusSqliteError) => void;
}

export interface PlusSqliteTransactionOptions {
  readonly name: string;
  readonly operation: "begin" | "commit" | "rollback";
  readonly success: () => void;
  readonly fail: (error: PlusSqliteError) => void;
}

export interface PlusSqliteCloseOptions {
  readonly name: string;
  readonly success: () => void;
  readonly fail: (error: PlusSqliteError) => void;
}

export interface PlusSqliteBridge {
  openDatabase(options: PlusSqliteOpenOptions): void;
  executeSql(options: PlusSqliteExecuteOptions): void;
  selectSql(options: PlusSqliteSelectOptions): void;
  transaction(options: PlusSqliteTransactionOptions): void;
  closeDatabase(options: PlusSqliteCloseOptions): void;
}

export function createAndroidSqliteExecutor(
  bridge: PlusSqliteBridge,
  databaseName: string = ANDROID_SQLITE_DATABASE_NAME,
  databasePath: string = ANDROID_SQLITE_DATABASE_PATH,
): SqlExecutor {
  let opening: Promise<void> | null = null;
  let closed = false;
  let depth = 0;

  const ensureOpen = (): Promise<void> => {
    if (closed) {
      return Promise.reject(new Error("Android SQLite 已关闭"));
    }
    if (!opening) {
      opening = callOpen(bridge, databaseName, databasePath).catch((error: unknown) => {
        opening = null;
        throw error;
      });
    }
    return opening;
  };

  return {
    async exec(sql: string): Promise<void> {
      await ensureOpen();
      await callExecute(bridge, databaseName, sql);
    },
    async run(sql: string, params: readonly SqlScalar[] = []): Promise<void> {
      await ensureOpen();
      await callExecute(bridge, databaseName, bindSql(sql, params));
    },
    async query(sql: string, params: readonly SqlScalar[] = []): Promise<readonly SqlRow[]> {
      await ensureOpen();
      const rows = await callSelect(bridge, databaseName, bindSql(sql, params));
      return rows.map((row) => toSqlRow(row));
    },
    async transaction<T>(work: () => Promise<T>): Promise<T> {
      await ensureOpen();
      if (depth > 0) {
        return work();
      }
      depth += 1;
      await callTransaction(bridge, databaseName, "begin");
      try {
        const result = await work();
        await callTransaction(bridge, databaseName, "commit");
        return result;
      } catch (error) {
        try {
          await callTransaction(bridge, databaseName, "rollback");
        } catch {
          // 回滚失败时仍抛出原来的错误，避免把事务说成已提交。
        }
        throw error;
      } finally {
        depth -= 1;
      }
    },
    async close(): Promise<void> {
      if (closed) {
        return;
      }
      closed = true;
      if (!opening) {
        return;
      }
      await callClose(bridge, databaseName);
    },
  };
}

function callOpen(bridge: PlusSqliteBridge, name: string, path: string): Promise<void> {
  return new Promise((resolve, reject) => {
    bridge.openDatabase({
      name,
      path,
      success: () => {
        resolve();
      },
      fail: (error) => {
        reject(plusFailure(error));
      },
    });
  });
}

function callExecute(bridge: PlusSqliteBridge, name: string, sql: string): Promise<void> {
  return new Promise((resolve, reject) => {
    bridge.executeSql({
      name,
      sql,
      success: () => {
        resolve();
      },
      fail: (error) => {
        reject(plusFailure(error));
      },
    });
  });
}

function callSelect(bridge: PlusSqliteBridge, name: string, sql: string): Promise<readonly unknown[]> {
  return new Promise((resolve, reject) => {
    bridge.selectSql({
      name,
      sql,
      success: (rows) => {
        if (!Array.isArray(rows)) {
          reject(new Error("Android SQLite 查询结果无法识别"));
          return;
        }
        resolve(rows);
      },
      fail: (error) => {
        reject(plusFailure(error));
      },
    });
  });
}

function callTransaction(
  bridge: PlusSqliteBridge,
  name: string,
  operation: "begin" | "commit" | "rollback",
): Promise<void> {
  return new Promise((resolve, reject) => {
    bridge.transaction({
      name,
      operation,
      success: () => {
        resolve();
      },
      fail: (error) => {
        reject(plusFailure(error));
      },
    });
  });
}

function callClose(bridge: PlusSqliteBridge, name: string): Promise<void> {
  return new Promise((resolve, reject) => {
    bridge.closeDatabase({
      name,
      success: () => {
        resolve();
      },
      fail: (error) => {
        reject(plusFailure(error));
      },
    });
  });
}

function plusFailure(error: PlusSqliteError): Error {
  const message = typeof error.message === "string" && error.message.length > 0 ? error.message : "Android SQLite 失败";
  return new Error(message);
}
