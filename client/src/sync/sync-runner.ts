import { DeviceRevokedError, NetworkError, RetryAfterError, SyncRejectedError, ApiResponseError } from "../api/errors";
import type { SyncTransport } from "../api/sync-transport";
import type { SyncPullQuery } from "../api/sync-types";
import { SYNC_PUSH_MAX_EVENTS } from "../api/sync-types";
import type { LearningRepository, OutboxAckResult } from "../repositories/types";
import { backoffDelayMs } from "./backoff";
import { SyncBusyError, SyncProtocolError, SyncRetryError } from "./errors";
import { splitSyncPushRequests } from "./push-batch";

export interface SyncRunner {
  push(streamEpoch: string): Promise<void>;
  pull(query: SyncPullQuery): Promise<void>;
}

export interface SyncRunnerOptions {
  readonly random?: () => number;
}

/**
 * 逐批 Push、逐条 ACK。失败不更换 eventId，也不因为 HTTP 200 清空未出现在结果里的事件。
 * 同一进程里 Push 与 Pull 不能重叠。
 */
export function createSyncRunner(
  repository: LearningRepository,
  transport: SyncTransport,
  options: SyncRunnerOptions = {},
): SyncRunner {
  let busy = false;
  let attemptIndex = 0;
  const random = options.random ?? Math.random;

  const run = async <T>(work: () => Promise<T>): Promise<T> => {
    if (busy) {
      throw new SyncBusyError();
    }
    busy = true;
    try {
      return await work();
    } finally {
      busy = false;
    }
  };

  return {
    push(streamEpoch: string): Promise<void> {
      return run(async () => {
        const seen = new Set<string>();
        const fail = (error: unknown): never => {
          throw mapSyncFailure(error, attemptIndex, random, () => {
            attemptIndex += 1;
          });
        };
        for (;;) {
          const pending = await repository.listOutbox(SYNC_PUSH_MAX_EVENTS);
          if (pending.length === 0) {
            attemptIndex = 0;
            return;
          }
          const signature = pending.map((entry) => entry.eventId).join("\n");
          if (seen.has(signature)) {
            throw new SyncProtocolError("outbox 没有前进");
          }
          seen.add(signature);
          let batches;
          try {
            batches = splitSyncPushRequests(pending, streamEpoch);
          } catch (error) {
            fail(error);
          }
          if (!batches) {
            throw new SyncProtocolError("Push 批次没有生成");
          }
          for (const batch of batches) {
            const allowed = new Set(batch.events.map((event) => event.eventId));
            let response;
            try {
              response = await transport.push(batch);
            } catch (error) {
              fail(error);
            }
            if (!response) {
              throw new SyncProtocolError("Push 响应缺失");
            }
            if (response.results.length === 0) {
              throw new SyncProtocolError("Push 响应没有逐条结果");
            }
            let acked = 0;
            for (const result of response.results) {
              if (!allowed.has(result.eventId)) {
                throw new SyncProtocolError("响应里的 eventId 不在本批 outbox 中");
              }
              await repository.ackOutbox(result.eventId, ackStatus(result.status, result.code));
              acked += 1;
            }
            if (acked === 0) {
              throw new SyncProtocolError("outbox 没有前进");
            }
          }
        }
      });
    },

    pull(query: SyncPullQuery): Promise<void> {
      return run(async () => {
        let cursor = query.cursor;
        const fail = (error: unknown): never => {
          throw mapSyncFailure(error, attemptIndex, random, () => {
            attemptIndex += 1;
          });
        };
        for (let page = 0; page < 1000; page += 1) {
          let response;
          try {
            response = await transport.pull({ ...query, cursor });
          } catch (error) {
            fail(error);
          }
          if (!response) {
            throw new SyncProtocolError("Pull 响应缺失");
          }
          await repository.savePullPage(response.changes, response.nextCursor);
          cursor = response.nextCursor;
          if (!response.hasMore) {
            attemptIndex = 0;
            return;
          }
        }
        throw new SyncProtocolError("Pull 页数异常");
      });
    },
  };
}

function ackStatus(status: OutboxAckResult, code: string | undefined): OutboxAckResult {
  if (code === "EVENT_ID_REUSED") {
    return "REJECTED";
  }
  return status;
}

function mapSyncFailure(error: unknown, attemptIndex: number, random: () => number, onRetry: () => void): Error {
  if (error instanceof DeviceRevokedError || error instanceof SyncRejectedError || error instanceof SyncProtocolError) {
    return error;
  }
  if (error instanceof RetryAfterError) {
    onRetry();
    return new SyncRetryError(error.delayMs, error);
  }
  if (error instanceof NetworkError || error instanceof ApiResponseError) {
    onRetry();
    return new SyncRetryError(backoffDelayMs(attemptIndex, unit(random)), error);
  }
  if (error instanceof Error) {
    return error;
  }
  onRetry();
  return new SyncRetryError(backoffDelayMs(attemptIndex, unit(random)), error);
}

function unit(random: () => number): number {
  const value = random();
  if (!Number.isFinite(value) || value < 0 || value >= 1) {
    return 0;
  }
  return value;
}
