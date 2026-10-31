import { DatabaseSync } from "node:sqlite";
import { toSqlRow, type SqlExecutor, type SqlRow, type SqlScalar } from "../src/repositories/sql-executor.ts";

export function createNodeSqliteExecutor(filename = ":memory:"): SqlExecutor {
  const db = new DatabaseSync(filename);
  let depth = 0;
  let closed = false;

  const guard = (): void => {
    if (closed) {
      throw new Error("sqlite 已关闭");
    }
  };

  return {
    async exec(sql: string): Promise<void> {
      guard();
      db.exec(sql);
    },
    async run(sql: string, params: readonly SqlScalar[] = []): Promise<void> {
      guard();
      db.prepare(sql).run(...params);
    },
    async query(sql: string, params: readonly SqlScalar[] = []): Promise<readonly SqlRow[]> {
      guard();
      const rows = db.prepare(sql).all(...params) as readonly unknown[];
      return rows.map((row) => toSqlRow(row));
    },
    async transaction<T>(work: () => Promise<T>): Promise<T> {
      guard();
      if (depth > 0) {
        return work();
      }
      depth += 1;
      db.exec("BEGIN");
      try {
        const result = await work();
        db.exec("COMMIT");
        return result;
      } catch (error) {
        try {
          db.exec("ROLLBACK");
        } catch {
          // 原错误优先。
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
      db.close();
    },
  };
}
