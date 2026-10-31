import { InjectedWriteFaultError, StorageCorruptError, StorageUnavailableError } from "./errors";
import { seedContractLessons } from "./lesson-catalog";
import {
  LOCAL_STORE_NAMES,
  type LocalDatabase,
  type LocalKey,
  type LocalRecords,
  type LocalStoreName,
  type LocalTransaction,
  type LocalTransactionMode,
  type WriteFaultInjection,
} from "./local-database";
import { parseStoreValue } from "./record-parse";

const DATABASE_NAME = "guoyu-english";
const DATABASE_VERSION = 1;

const KEY_PATH: { readonly [K in LocalStoreName]: string } = {
  progress: "progressKey",
  outbox: "eventId",
  estimatedStar: "estimateKey",
  pullChange: "seq",
  pullCursor: "id",
  lesson: "lessonKey",
  ackJournal: "eventId",
};

export interface IndexedDbOpenOptions {
  readonly databaseName?: string;
  readonly factory?: IDBFactory;
}

export async function openIndexedDbDatabase(
  options: IndexedDbOpenOptions = {},
): Promise<LocalDatabase & WriteFaultInjection> {
  const connection = await openConnection(options.databaseName ?? DATABASE_NAME, options.factory ?? requireIndexedDb());
  const database = new IndexedDbDatabase(connection);
  try {
    await seedContractLessons(database);
  } catch (error) {
    database.closeSilently();
    throw error;
  }
  return database;
}

function openConnection(databaseName: string, factory: IDBFactory): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = factory.open(databaseName, DATABASE_VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      for (const name of LOCAL_STORE_NAMES) {
        if (!db.objectStoreNames.contains(name)) {
          db.createObjectStore(name, { keyPath: KEY_PATH[name] });
        }
      }
    };
    request.onsuccess = () => {
      resolve(request.result);
    };
    request.onerror = () => {
      reject(request.error ?? new StorageUnavailableError());
    };
  });
}

function requireIndexedDb(): IDBFactory {
  if (typeof indexedDB === "undefined") {
    throw new StorageUnavailableError();
  }
  return indexedDB;
}

class IndexedDbDatabase implements LocalDatabase, WriteFaultInjection {
  private tail: Promise<void> = Promise.resolve();
  private pendingFault: number | null = null;
  private closed = false;
  private readonly connection: IDBDatabase;

  constructor(connection: IDBDatabase) {
    this.connection = connection;
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
    this.connection.close();
    return Promise.resolve();
  }

  closeSilently(): void {
    this.closed = true;
    this.connection.close();
  }

  private takeFault(mode: LocalTransactionMode): number | null {
    if (mode !== "readwrite") {
      return null;
    }
    const fault = this.pendingFault;
    this.pendingFault = null;
    return fault;
  }

  private async runTransaction<T>(
    mode: LocalTransactionMode,
    work: (tx: LocalTransaction) => Promise<T>,
  ): Promise<T> {
    const tx = this.connection.transaction([...LOCAL_STORE_NAMES], mode);
    const completed = waitTransaction(tx);
    void completed.catch(() => undefined);
    const fault = this.takeFault(mode);
    let mutations = 0;
    const noteMutate = (): void => {
      mutations += 1;
      if (fault !== null && mutations === fault) {
        throw new InjectedWriteFaultError();
      }
    };
    const localTx: LocalTransaction = {
      get: (store, key) => readStore(tx, store, key),
      put: async (store, value) => {
        const parsed = parseStoreValue(store, value);
        noteMutate();
        await writeStore(tx, store, parsed);
      },
      delete: async (store, key) => {
        noteMutate();
        await deleteStore(tx, store, key);
      },
      list: (store) => listStore(tx, store),
    };
    try {
      const result = await work(localTx);
      await completed;
      return result;
    } catch (error) {
      abortTransaction(tx);
      await completed.catch(() => undefined);
      throw error;
    }
  }
}

function waitTransaction(tx: IDBTransaction): Promise<void> {
  return new Promise((resolve, reject) => {
    tx.oncomplete = () => {
      resolve();
    };
    tx.onabort = () => {
      reject(tx.error ?? new Error("IndexedDB 事务已回滚"));
    };
    tx.onerror = () => {
      reject(tx.error ?? new Error("IndexedDB 事务失败"));
    };
  });
}

function abortTransaction(tx: IDBTransaction): void {
  try {
    tx.abort();
  } catch {
    // 事务可能已经结束。
  }
}

function waitUnknown(request: IDBRequest): Promise<unknown> {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => {
      resolve(request.result as unknown);
    };
    request.onerror = () => {
      reject(request.error ?? new Error("IndexedDB 请求失败"));
    };
  });
}

async function readStore<K extends LocalStoreName>(
  tx: IDBTransaction,
  store: K,
  key: LocalKey,
): Promise<LocalRecords[K] | undefined> {
  const raw = await waitUnknown(tx.objectStore(store).get(key));
  if (raw === undefined) {
    return undefined;
  }
  return parseStoreValue(store, raw);
}

async function writeStore<K extends LocalStoreName>(
  tx: IDBTransaction,
  store: K,
  value: LocalRecords[K],
): Promise<void> {
  await waitUnknown(tx.objectStore(store).put(value));
}

async function deleteStore(tx: IDBTransaction, store: LocalStoreName, key: LocalKey): Promise<void> {
  await waitUnknown(tx.objectStore(store).delete(key));
}

async function listStore<K extends LocalStoreName>(
  tx: IDBTransaction,
  store: K,
): Promise<readonly LocalRecords[K][]> {
  const rows: LocalRecords[K][] = [];
  await new Promise<void>((resolve, reject) => {
    const request = tx.objectStore(store).openCursor();
    request.onsuccess = () => {
      const cursor = request.result;
      if (!cursor) {
        resolve();
        return;
      }
      try {
        rows.push(parseStoreValue(store, cursorValue(cursor)));
      } catch (error) {
        reject(error);
        return;
      }
      cursor.continue();
    };
    request.onerror = () => {
      reject(request.error ?? new StorageCorruptError(store));
    };
  });
  return rows;
}

function cursorValue(cursor: IDBCursorWithValue): unknown {
  return cursor.value as unknown;
}
