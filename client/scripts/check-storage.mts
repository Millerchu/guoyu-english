import "fake-indexeddb/auto";

import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { describe, test } from "node:test";
import { DatabaseSync } from "node:sqlite";

import {
  DeviceRevokedError,
  NetworkError,
  RetryAfterError,
  SyncRejectedError,
} from "../src/api/errors.ts";
import { retryAfterDelayMs } from "../src/api/sync-transport.ts";
import { createSyncHttpTransport } from "../src/api/sync-transport.ts";
import type { FetchLike, FetchResponse, SyncTransport } from "../src/api/sync-transport.ts";
import type { SyncPushRequest } from "../src/api/sync-types.ts";
import {
  ANDROID_SQLITE_DATABASE_NAME,
  ANDROID_SQLITE_DATABASE_PATH,
  ANDROID_SQLITE_DEVICE_VERIFIED,
  createAndroidSqliteExecutor,
} from "../src/repositories/android-sqlite.ts";
import type { PlusSqliteBridge } from "../src/repositories/android-sqlite.ts";
import {
  EventIdReusedError,
  LearningActionNotSavedError,
  LessonNotFoundError,
  PullPageNotSavedError,
  StorageReadError,
  StorageUnavailableError,
} from "../src/repositories/errors.ts";
import { openIndexedDbDatabase } from "../src/repositories/indexeddb-database.ts";
import { createLearningRepository } from "../src/repositories/learning-repository.ts";
import type { LocalDatabase, WriteFaultInjection } from "../src/repositories/local-database.ts";
import { detectPlatformKind } from "../src/repositories/platform.ts";
import { feedbackForSaveFailure } from "../src/repositories/save-feedback.ts";
import { bindSql } from "../src/repositories/sql-executor.ts";
import { openSqliteDatabase } from "../src/repositories/sqlite-database.ts";
import type { LearningRepository, SyncChange } from "../src/repositories/types.ts";
import { backoffDelayMs } from "../src/sync/backoff.ts";
import { SyncBusyError, SyncRetryError } from "../src/sync/errors.ts";
import { splitSyncPushRequests, syncPushRequestBytes } from "../src/sync/push-batch.ts";
import { createSyncRunner } from "../src/sync/sync-runner.ts";
import { createNodeSqliteExecutor } from "./node-sqlite-executor.mts";

const fixedNow = (): Date => new Date("2026-10-31T00:00:00.000Z");

async function rejected(work: () => Promise<unknown>): Promise<unknown> {
  try {
    await work();
  } catch (error) {
    return error;
  }
  throw new Error("应当拒绝");
}

interface Session {
  readonly db: LocalDatabase & WriteFaultInjection;
  readonly repository: LearningRepository;
  close(): Promise<void>;
  reopen(): Promise<Session>;
}

function track(initial: Session): Session {
  let current = initial;
  const tracked: Session = {
    get db() {
      return current.db;
    },
    get repository() {
      return current.repository;
    },
    async reopen() {
      current = await current.reopen();
      return tracked;
    },
    close: () => current.close(),
  };
  return tracked;
}

function repositoryFor(db: LocalDatabase, now: () => Date = fixedNow): LearningRepository {
  return createLearningRepository(db, { now });
}

async function openIndexed(name: string): Promise<Session> {
  const db = await openIndexedDbDatabase({ databaseName: name });
  return {
    db,
    repository: repositoryFor(db),
    close: () => db.close(),
    reopen: async () => {
      await db.close();
      return openIndexed(name);
    },
  };
}

async function openNode(path: string): Promise<Session> {
  const db = await openSqliteDatabase(createNodeSqliteExecutor(path));
  return {
    db,
    repository: repositoryFor(db),
    close: () => db.close(),
    reopen: async () => {
      await db.close();
      return openNode(path);
    },
  };
}

async function openAndroid(path: string): Promise<Session> {
  const native = new DatabaseSync(path);
  const fake = createFakePlus(native);
  const db = await openSqliteDatabase(createAndroidSqliteExecutor(fake.bridge));
  return {
    db,
    repository: repositoryFor(db),
    close: () => db.close(),
    reopen: async () => {
      await db.close();
      return openAndroid(path);
    },
  };
}

async function withKind(kind: "indexeddb" | "sqlite" | "android", work: (session: Session) => Promise<void>): Promise<void> {
  if (kind === "indexeddb") {
    const name = `guoyu-s09-${randomUUID()}`;
    const session = track(await openIndexed(name));
    try {
      await work(session);
    } finally {
      await session.close();
      await deleteIndexedDb(name);
    }
    return;
  }
  const dir = await mkdtemp(join(tmpdir(), "guoyu-s09-"));
  try {
    const session = track(await (kind === "sqlite" ? openNode(join(dir, "app.db")) : openAndroid(join(dir, "app.db"))));
    try {
      await work(session);
    } finally {
      await session.close();
    }
  } finally {
    await rm(dir, { recursive: true, force: true });
  }
}

async function deleteIndexedDb(name: string): Promise<void> {
  await new Promise<void>((resolve, reject) => {
    const request = indexedDB.deleteDatabase(name);
    request.onsuccess = () => {
      resolve();
    };
    request.onerror = () => {
      reject(request.error ?? new Error("deleteDatabase failed"));
    };
    request.onblocked = () => {
      resolve();
    };
  });
}

const speak = {
  eventId: "evt-retry",
  stepId: "step-speak" as const,
  targetId: "sent-i-see-a-cat",
};

for (const kind of ["indexeddb", "sqlite", "android"] as const) {
  describe(kind, () => {
    test("事务失败不留半条 outbox，重试不换 eventId", async () => {
      await withKind(kind, async (session) => {
        session.db.armWriteFault(3);
        const error = await rejected(() => session.repository.saveLearningAction(speak));
        assert.ok(error instanceof LearningActionNotSavedError);
        assert.equal(error.saved, false);
        const feedback = feedbackForSaveFailure(error);
        assert.equal(feedback.saved, false);
        assert.equal(feedback.message.includes("已保存"), false);
        assert.equal(error.message.includes("已保存"), false);
        assert.deepEqual(await session.repository.listOutbox(10), []);
        assert.equal((await session.db.transaction("readonly", (tx) => tx.list("progress"))).length, 0);
        assert.equal((await session.db.transaction("readonly", (tx) => tx.list("estimatedStar"))).length, 0);

        await session.repository.saveLearningAction(speak);
        await session.repository.saveLearningAction(speak);
        const listed = await session.repository.listOutbox(10);
        assert.equal(listed.length, 1);
        assert.equal(listed[0]?.eventId, "evt-retry");
        assert.equal(listed[0]?.eventType, "STEP_FINISHED");
        assert.equal(listed[0]?.targetId, "sent-i-see-a-cat");
        const stars = await session.db.transaction("readonly", (tx) => tx.list("estimatedStar"));
        assert.equal(stars.length, 1);
        assert.equal(stars[0]?.eventId, "evt-retry");
        assert.equal(stars[0]?.state, "PENDING");
      });
    });

    test("Pull 写到一半不推进游标", async () => {
      await withKind(kind, async (session) => {
        await session.repository.savePullPage([{}], "cursor-1");
        session.db.armWriteFault(2);
        const error = await rejected(() => session.repository.savePullPage([{}, {}], "cursor-2"));
        assert.ok(error instanceof PullPageNotSavedError);
        assert.equal(error.saved, false);
        const cursor = await session.db.transaction("readonly", (tx) => tx.get("pullCursor", "local"));
        assert.equal(cursor?.nextCursor, "cursor-1");
        const changes = await session.db.transaction("readonly", (tx) => tx.list("pullChange"));
        assert.equal(changes.length, 1);
        assert.equal(changes[0]?.json, "{}");

        await session.repository.savePullPage([{}], "cursor-2");
        const advanced = await session.db.transaction("readonly", (tx) => tx.get("pullCursor", "local"));
        assert.equal(advanced?.nextCursor, "cursor-2");
        assert.equal((await session.db.transaction("readonly", (tx) => tx.list("pullChange"))).length, 2);
      });
    });

    test("重新打开后仍是原来的 eventId", async () => {
      await withKind(kind, async (session) => {
        await session.repository.saveLearningAction({
          eventId: "evt-keep",
          stepId: "step-listen",
          targetId: "lexeme-cat",
        });
        await session.reopen();
        const listed = await session.repository.listOutbox(5);
        assert.equal(listed.length, 1);
        assert.equal(listed[0]?.eventId, "evt-keep");
        const lesson = await session.repository.getLesson("animals-001", 1);
        assert.equal(lesson.lessonId, "animals-001");
        assert.equal(lesson.version, 1);
        assert.deepEqual(
          lesson.steps.map((step) => step.stepId),
          ["step-listen", "step-speak", "step-recognize", "step-read"],
        );
      });
    });
  });
}

test("相同 eventId 不改载荷，听和读不预估星星，确认不删预估", async () => {
  const db = await openSqliteDatabase(createNodeSqliteExecutor());
  try {
    let ticks = 0;
    const repository = createLearningRepository(db, {
      now: () => new Date(Date.UTC(2026, 9, 31, 0, 0, ticks++)),
    });
    await repository.saveLearningAction({ eventId: "evt-1", stepId: "step-listen" });
    await assert.rejects(
      () => repository.saveLearningAction({ eventId: "evt-1", stepId: "step-recognize" }),
      EventIdReusedError,
    );
    const listed = await repository.listOutbox(10);
    assert.equal(listed.length, 1);
    assert.equal(listed[0]?.eventId, "evt-1");
    assert.equal(listed[0]?.stepId, "step-listen");
    const createdAt = (await db.transaction("readonly", (tx) => tx.list("outbox")))[0]?.createdAt;
    await repository.saveLearningAction({ eventId: "evt-1", stepId: "step-listen" });
    const afterRetry = await db.transaction("readonly", (tx) => tx.list("outbox"));
    assert.equal(afterRetry.length, 1);
    assert.equal(afterRetry[0]?.createdAt, createdAt);
    assert.equal(afterRetry[0]?.eventId, "evt-1");

    await repository.saveLearningAction({ eventId: "evt-speak-1", stepId: "step-speak", targetId: "sent-i-see-a-cat" });
    await repository.saveLearningAction({ eventId: "evt-speak-2", stepId: "step-speak", targetId: "sent-i-see-a-cat" });
    await repository.saveLearningAction({ eventId: "evt-read", stepId: "step-read", targetId: "sent-i-see-a-dog" });
    const stars = await db.transaction("readonly", (tx) => tx.list("estimatedStar"));
    assert.equal(stars.length, 1);
    assert.equal(stars[0]?.eventId, "evt-speak-1");

    await assert.rejects(
      () => repository.saveLearningAction({ eventId: "evt-no-sentence", stepId: "step-speak" }),
      (error: unknown) => error instanceof Error && error.message.includes("没有保存") && !error.message.includes("已保存"),
    );
    assert.equal((await repository.listOutbox(10)).some((entry) => entry.eventId === "evt-no-sentence"), false);

    await repository.ackOutbox("evt-speak-1", "ACCEPTED");
    const pending = (await repository.listOutbox(10)).map((entry) => entry.eventId);
    assert.deepEqual(pending.includes("evt-speak-1"), false);
    assert.equal(pending.includes("evt-speak-2"), true);
    assert.equal((await db.transaction("readonly", (tx) => tx.list("estimatedStar"))).length, 1);
    await repository.ackOutbox("evt-speak-1", "DUPLICATE");
    const journal = await db.transaction("readonly", (tx) => tx.get("ackJournal", "evt-speak-1"));
    assert.equal(journal?.eventId, "evt-speak-1");
    assert.equal(journal?.result, "ACCEPTED");

    db.armWriteFault(2);
    await assert.rejects(() => repository.ackOutbox("evt-speak-2", "REJECTED"));
    assert.equal((await repository.listOutbox(10)).some((entry) => entry.eventId === "evt-speak-2"), true);
    assert.equal(await db.transaction("readonly", (tx) => tx.get("ackJournal", "evt-speak-2")), undefined);

    await repository.ackOutbox("evt-speak-2", "REJECTED");
    assert.equal((await repository.listOutbox(10)).some((entry) => entry.eventId === "evt-speak-2"), false);
    const rejected = await db.transaction("readonly", (tx) => tx.get("outbox", "evt-speak-2"));
    assert.equal(rejected?.eventId, "evt-speak-2");
    assert.equal(rejected?.status, "REJECTED");

    await assert.rejects(() => repository.getLesson("animals-001", 3), LessonNotFoundError);
    await assert.rejects(
      () => repository.savePullPage([1 as unknown as SyncChange], "cursor-9"),
      (error: unknown) => {
        assert.ok(error instanceof Error);
        assert.equal(error.message.includes("已保存"), false);
        return true;
      },
    );
    assert.equal(await db.transaction("readonly", (tx) => tx.get("pullCursor", "local")), undefined);
  } finally {
    await db.close();
  }
});

test("关闭数据库后不能把队列读成空列表", async () => {
  const db = await openSqliteDatabase(createNodeSqliteExecutor());
  const repository = repositoryFor(db);
  await repository.saveLearningAction({ eventId: "evt-1", stepId: "step-listen" });
  await db.close();
  await assert.rejects(() => repository.listOutbox(10), StorageReadError);
});

test("Push 失败和部分 ACK 都复用原来的 eventId", async () => {
  const db = await openSqliteDatabase(createNodeSqliteExecutor());
  try {
    const repository = repositoryFor(db);
    await repository.saveLearningAction({ eventId: "evt-a", stepId: "step-listen", targetId: "lexeme-cat" });
    await repository.saveLearningAction({ eventId: "evt-b", stepId: "step-listen", targetId: "lexeme-dog" });
    const seen: string[][] = [];
    let calls = 0;
    const transport: SyncTransport = {
      async push(request: SyncPushRequest) {
        calls += 1;
        seen.push(request.events.map((event) => event.eventId));
        assert.equal(request.schemaVersion, 2);
        if (calls === 1) {
          throw new NetworkError();
        }
        const first = request.events[0];
        if (!first) {
          throw new Error("empty batch");
        }
        if (calls === 2) {
          return { results: [{ eventId: first.eventId, status: "ACCEPTED" }] };
        }
        return {
          results: request.events.map((event) => ({ eventId: event.eventId, status: "ACCEPTED" as const })),
        };
      },
      async pull() {
        throw new Error("pull 不应被调用");
      },
    };
    const runner = createSyncRunner(repository, transport, { random: () => 0 });
    const retry = await rejected(() => runner.push("epoch-1"));
    assert.ok(retry instanceof SyncRetryError);
    assert.equal(retry.delayMs, 5000);
    assert.equal(retry.message.includes("已保存"), false);
    assert.deepEqual(
      (await repository.listOutbox(10)).map((entry) => entry.eventId),
      ["evt-a", "evt-b"],
    );
    await runner.push("epoch-1");
    assert.deepEqual(seen, [["evt-a", "evt-b"], ["evt-a", "evt-b"], ["evt-b"]]);
    assert.deepEqual(await repository.listOutbox(10), []);
    const stars = await db.transaction("readonly", (tx) => tx.list("estimatedStar"));
    assert.equal(stars.length, 0);
  } finally {
    await db.close();
  }
});

test("EVENT_ID_REUSED、设备撤销和限流都不新编 eventId", async () => {
  const db = await openSqliteDatabase(createNodeSqliteExecutor());
  try {
    const repository = repositoryFor(db);
    await repository.saveLearningAction({ eventId: "evt-keep", stepId: "step-speak", targetId: "sent-i-see-a-dog" });
    const runner = createSyncRunner(
      repository,
      {
        async push(request) {
          return {
            results: request.events.map((event) => ({
              eventId: event.eventId,
              status: "ACCEPTED" as const,
              code: "EVENT_ID_REUSED" as const,
            })),
          };
        },
        async pull() {
          throw new Error("unused");
        },
      },
      { random: () => 0 },
    );
    await runner.push("epoch-1");
    assert.deepEqual(await repository.listOutbox(10), []);
    const kept = await db.transaction("readonly", (tx) => tx.get("outbox", "evt-keep"));
    assert.equal(kept?.eventId, "evt-keep");
    assert.equal(kept?.status, "REJECTED");
    assert.equal((await db.transaction("readonly", (tx) => tx.list("estimatedStar"))).length, 1);

    await repository.saveLearningAction({ eventId: "evt-revoked", stepId: "step-listen" });
    const revoked = createSyncRunner(repository, {
      async push() {
        throw new DeviceRevokedError();
      },
      async pull() {
        throw new Error("unused");
      },
    });
    await assert.rejects(() => revoked.push("epoch-1"), DeviceRevokedError);
    assert.equal((await repository.listOutbox(10)).some((entry) => entry.eventId === "evt-revoked"), true);

    const limited = createSyncRunner(
      repository,
      {
        async push() {
          throw new RetryAfterError(12_000);
        },
        async pull() {
          throw new Error("unused");
        },
      },
      { random: () => 0 },
    );
    const retry = await rejected(() => limited.push("epoch-1"));
    assert.ok(retry instanceof SyncRetryError);
    assert.equal(retry.delayMs, 12_000);
    assert.equal((await repository.listOutbox(10)).some((entry) => entry.eventId === "evt-revoked"), true);
  } finally {
    await db.close();
  }
});

test("Pull 第二页失败时停在上一页游标", async () => {
  const db = await openSqliteDatabase(createNodeSqliteExecutor());
  try {
    const repository = repositoryFor(db);
    let calls = 0;
    const runner = createSyncRunner(repository, {
      async push() {
        throw new Error("unused");
      },
      async pull() {
        calls += 1;
        if (calls === 1) {
          return {
            changes: [{ seq: 1 } as unknown as SyncChange],
            nextCursor: "cursor-1",
            hasMore: true,
          };
        }
        throw new NetworkError();
      },
    });
    await assert.rejects(
      () => runner.pull({ childId: "child-1", streamEpoch: "epoch-1", cursor: "", limit: 100 }),
      SyncRetryError,
    );
    const cursor = await db.transaction("readonly", (tx) => tx.get("pullCursor", "local"));
    assert.equal(cursor?.nextCursor, "cursor-1");
    const stored = await db.transaction("readonly", (tx) => tx.list("pullChange"));
    assert.equal(stored.length, 1);
    assert.equal(stored[0]?.json.includes("seq"), true);
  } finally {
    await db.close();
  }
});

test("同步忙时第二次调用不会重发", async () => {
  const db = await openSqliteDatabase(createNodeSqliteExecutor());
  try {
    const repository = repositoryFor(db);
    await repository.saveLearningAction({ eventId: "evt-1", stepId: "step-listen" });
    let release: (() => void) | undefined;
    const gate = new Promise<void>((resolve) => {
      release = resolve;
    });
    const runner = createSyncRunner(repository, {
      async push() {
        await gate;
        return { results: [{ eventId: "evt-1", status: "ACCEPTED" }] };
      },
      async pull() {
        throw new Error("unused");
      },
    });
    const first = runner.push("epoch-1");
    await assert.rejects(() => runner.push("epoch-1"), SyncBusyError);
    release?.();
    await first;
    assert.deepEqual(await repository.listOutbox(10), []);
  } finally {
    await db.close();
  }
});

test("HTTP Push 带 schemaVersion 和 eventId，错误信息不含令牌", async () => {
  const token = "device-token-test";
  const requests: { url: string; init: { method: string; headers: Readonly<Record<string, string>>; body?: string } }[] = [];
  const fetchImpl: FetchLike = async (url, init) => {
    requests.push({ url, init });
    if (requests.length === 1) {
      return jsonResponse(200, {
        results: [{ eventId: "evt-1", status: "ACCEPTED" }],
      });
    }
    throw new Error("network down");
  };
  const transport = createSyncHttpTransport({
    baseUrl: "http://127.0.0.1:9",
    deviceToken: token,
    fetchImpl,
  });
  const pushed = await transport.push({
    schemaVersion: 2,
    streamEpoch: "epoch-1",
    events: [{ eventId: "evt-1", eventType: "STEP_FINISHED", stepId: "step-listen" }],
  });
  assert.equal(pushed.results[0]?.eventId, "evt-1");
  assert.equal(requests[0]?.url, "http://127.0.0.1:9/api/v1/sync/push");
  assert.equal(requests[0]?.init.method, "POST");
  assert.equal(requests[0]?.init.headers.Authorization, `Bearer ${token}`);
  assert.equal(requests[0]?.init.body?.includes('"schemaVersion":2'), true);
  assert.equal(requests[0]?.init.body?.includes('"eventId":"evt-1"'), true);
  const failure = await rejected(() =>
    transport.push({
      schemaVersion: 2,
      streamEpoch: "epoch-1",
      events: [{ eventId: "evt-1", eventType: "STEP_FINISHED", stepId: "step-listen" }],
    }),
  );
  assert.ok(failure instanceof NetworkError);
  assert.equal(String(failure).includes(token), false);

  const denied = createSyncHttpTransport({
    baseUrl: "http://127.0.0.1:9",
    deviceToken: token,
    fetchImpl: async () => jsonResponse(401, {}, "DEVICE_REVOKED"),
  });
  await assert.rejects(() => denied.pull({ childId: "child-1", streamEpoch: "epoch-1", cursor: "", limit: 100 }), DeviceRevokedError);

  const limited = createSyncHttpTransport({
    baseUrl: "http://127.0.0.1:9",
    fetchImpl: async () => jsonResponse(429, {}, "OK", { "retry-after": "12" }),
  });
  const limitedError = await rejected(() =>
    limited.pull({ childId: "child-1", streamEpoch: "epoch-1", cursor: "0", limit: 100 }),
  );
  assert.ok(limitedError instanceof RetryAfterError);
  assert.equal(limitedError.delayMs, 12_000);

  const rejectedTransport = createSyncHttpTransport({
    baseUrl: "http://127.0.0.1:9",
    fetchImpl: async () => jsonResponse(400, {}, "INVALID_ARGUMENT"),
  });
  const rejectedError = await rejected(() =>
    rejectedTransport.pull({ childId: "child-1", streamEpoch: "epoch-1", cursor: "0", limit: 100 }),
  );
  assert.ok(rejectedError instanceof SyncRejectedError);
  assert.equal(rejectedError.reason, "INVALID_ARGUMENT");
  assert.equal(rejectedError.message.includes(token), false);
});

test("拆批保持 eventId，退避不超过 5 分钟", () => {
  const entries = [0, 1, 2, 3, 4].map((index) => ({
    eventId: `evt-${index}`,
    eventType: "STEP_FINISHED" as const,
    stepId: "step-listen" as const,
  }));
  const batches = splitSyncPushRequests(entries, "epoch-1", { maxEvents: 2 });
  assert.deepEqual(
    batches.map((batch) => batch.events.map((event) => event.eventId)),
    [["evt-0", "evt-1"], ["evt-2", "evt-3"], ["evt-4"]],
  );
  assert.equal(batches[0]?.schemaVersion, 2);
  const one = splitSyncPushRequests([entries[0]!], "epoch-1");
  const first = one[0];
  assert.ok(first);
  const width = syncPushRequestBytes(first);
  const byByte = splitSyncPushRequests(entries, "epoch-1", { maxBytes: width });
  assert.deepEqual(
    byByte.map((batch) => batch.events.map((event) => event.eventId)),
    [["evt-0"], ["evt-1"], ["evt-2"], ["evt-3"], ["evt-4"]],
  );
  assert.throws(() => splitSyncPushRequests(entries, "epoch-1", { maxBytes: 8 }));
  assert.equal(backoffDelayMs(0, 0), 5_000);
  assert.equal(backoffDelayMs(1, 0), 15_000);
  assert.equal(backoffDelayMs(2, 0), 45_000);
  assert.equal(backoffDelayMs(3, 0), 300_000);
  assert.equal(backoffDelayMs(9, 0), 300_000);
  const jittered = backoffDelayMs(0, 0.5);
  assert.ok(jittered < 5_000 && jittered >= 4_000);
  assert.equal(retryAfterDelayMs("12"), 12_000);
  assert.equal(retryAfterDelayMs("soon"), null);
  assert.equal(bindSql("VALUES (?, ?)", ["o'brien", null]), "VALUES ('o''brien', NULL)");
});

test("有 plus.sqlite 时不退回 IndexedDB，且标明没有真机验证", async () => {
  assert.equal(ANDROID_SQLITE_DEVICE_VERIFIED, false);
  const bridge = fakeBridge();
  assert.equal(detectPlatformKind({ plus: { sqlite: bridge }, indexedDB: indexedDB }), "android-sqlite");
  assert.equal(detectPlatformKind({ indexedDB }), "indexeddb");
  const missing = detectPlatformKindSafe({});
  assert.ok(missing instanceof StorageUnavailableError);
  assert.equal(missing.saved, false);

  const dir = await mkdtemp(join(tmpdir(), "guoyu-s09-"));
  try {
    const native = new DatabaseSync(join(dir, "app.db"));
    const fake = createFakePlus(native);
    const db = await openSqliteDatabase(createAndroidSqliteExecutor(fake.bridge));
    assert.deepEqual(fake.opened, [{ name: ANDROID_SQLITE_DATABASE_NAME, path: ANDROID_SQLITE_DATABASE_PATH }]);
    await db.close();
  } finally {
    await rm(dir, { recursive: true, force: true });
  }
});

function detectPlatformKindSafe(host: Parameters<typeof detectPlatformKind>[0]): StorageUnavailableError | string {
  try {
    return detectPlatformKind(host);
  } catch (error) {
    if (error instanceof StorageUnavailableError) {
      return error;
    }
    throw error;
  }
}

function jsonResponse(
  status: number,
  data: unknown,
  code = "OK",
  headers: Readonly<Record<string, string>> = {},
): FetchResponse {
  const body = JSON.stringify({
    requestId: "req-1",
    code,
    message: code,
    data,
    serverTime: "2026-10-31T00:00:00.000Z",
  });
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: {
      get(name: string) {
        return headers[name.toLowerCase()] ?? null;
      },
    },
    text: () => Promise.resolve(body),
  };
}

function fakeBridge(): PlusSqliteBridge {
  return {
    openDatabase: (options) => {
      options.success();
    },
    executeSql: (options) => {
      options.success();
    },
    selectSql: (options) => {
      options.success([]);
    },
    transaction: (options) => {
      options.success();
    },
    closeDatabase: (options) => {
      options.success();
    },
  };
}

function createFakePlus(db: DatabaseSync): { bridge: PlusSqliteBridge; opened: { name: string; path: string }[] } {
  const opened: { name: string; path: string }[] = [];
  const bridge: PlusSqliteBridge = {
    openDatabase(options) {
      opened.push({ name: options.name, path: options.path });
      options.success();
    },
    executeSql(options) {
      try {
        db.exec(options.sql);
        options.success();
      } catch (error) {
        options.fail({ message: error instanceof Error ? error.message : "execute failed" });
      }
    },
    selectSql(options) {
      try {
        options.success(db.prepare(options.sql).all() as readonly unknown[]);
      } catch (error) {
        options.fail({ message: error instanceof Error ? error.message : "select failed" });
      }
    },
    transaction(options) {
      try {
        if (options.operation === "begin") {
          db.exec("BEGIN");
        } else if (options.operation === "commit") {
          db.exec("COMMIT");
        } else {
          db.exec("ROLLBACK");
        }
        options.success();
      } catch (error) {
        options.fail({ message: error instanceof Error ? error.message : "transaction failed" });
      }
    },
    closeDatabase(options) {
      try {
        db.close();
        options.success();
      } catch (error) {
        options.fail({ message: error instanceof Error ? error.message : "close failed" });
      }
    },
  };
  return { bridge, opened };
}
