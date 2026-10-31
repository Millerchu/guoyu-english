export type SqlScalar = string | number | null;

export type SqlRow = Readonly<Record<string, SqlScalar>>;

/**
 * 参数化 SQL。Android 适配会先绑定参数再交给 plus.sqlite；
 * 测试里的 node:sqlite 直接使用占位符。两边执行同一条语句文本。
 */
export interface SqlExecutor {
  exec(sql: string): Promise<void>;
  run(sql: string, params?: readonly SqlScalar[]): Promise<void>;
  query(sql: string, params?: readonly SqlScalar[]): Promise<readonly SqlRow[]>;
  transaction<T>(work: () => Promise<T>): Promise<T>;
  close(): Promise<void>;
}

export function isSqlRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function normalizeSqlScalar(value: unknown): SqlScalar {
  if (value === null || value === undefined) {
    return null;
  }
  if (typeof value === "string") {
    return value;
  }
  if (typeof value === "number") {
    return value;
  }
  if (typeof value === "bigint") {
    const asNumber = Number(value);
    if (!Number.isSafeInteger(asNumber) || BigInt(asNumber) !== value) {
      throw new Error("SQLite 整数超出安全范围");
    }
    return asNumber;
  }
  throw new Error("SQLite 字段类型无法识别");
}

export function toSqlRow(value: unknown): SqlRow {
  if (!isSqlRecord(value)) {
    throw new Error("SQLite 行无法识别");
  }
  const row: Record<string, SqlScalar> = {};
  for (const key of Object.keys(value)) {
    row[key] = normalizeSqlScalar(value[key]);
  }
  return row;
}

export function sqlLiteral(value: SqlScalar): string {
  if (value === null) {
    return "NULL";
  }
  if (typeof value === "number") {
    if (!Number.isSafeInteger(value)) {
      throw new Error("SQL 数字必须是整数");
    }
    return String(value);
  }
  if (value.includes("\u0000")) {
    throw new Error("SQL 文本含有空字符");
  }
  return `'${value.replace(/'/g, "''")}'`;
}

/** 把 `?` 换成已转义字面量。语句里不要写字符串中的问号。 */
export function bindSql(sql: string, params: readonly SqlScalar[] = []): string {
  let index = 0;
  const bound = sql.replace(/\?/g, () => {
    const value = params[index];
    if (value === undefined || index >= params.length) {
      throw new Error("SQL 参数不足");
    }
    index += 1;
    return sqlLiteral(value);
  });
  if (index !== params.length) {
    throw new Error("SQL 参数过多");
  }
  return bound;
}
